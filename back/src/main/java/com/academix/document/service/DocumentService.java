package com.academix.document.service;

import com.academix.classroom.entity.SchoolClass;
import com.academix.document.dto.DocumentResponse;
import com.academix.document.dto.GenerateDocumentRequest;
import com.academix.document.entity.AdministrativeDocument;
import com.academix.document.repository.DocumentRepository;
import com.academix.grade.entity.Grade;
import com.academix.grade.entity.Semester;
import com.academix.grade.repository.GradeRepository;
import com.academix.student.entity.Student;
import com.academix.student.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final DocumentRepository documentRepository;
	private final StudentRepository studentRepository;
	private final GradeRepository gradeRepository;
	private final DocumentStorageService storageService;
	private final String institutionName;
	private final String institutionAddress;

	public DocumentService(DocumentRepository documentRepository, StudentRepository studentRepository,
			GradeRepository gradeRepository, DocumentStorageService storageService,
			@Value("${app.institution.name}") String institutionName,
			@Value("${app.institution.address}") String institutionAddress) {
		this.documentRepository = documentRepository;
		this.studentRepository = studentRepository;
		this.gradeRepository = gradeRepository;
		this.storageService = storageService;
		this.institutionName = institutionName;
		this.institutionAddress = institutionAddress;
	}

	@Transactional(readOnly = true)
	public List<DocumentResponse> getAll(Long studentId) {
		requireAdministration();
		List<AdministrativeDocument> documents = studentId != null
				? documentRepository.findByStudentId(studentId)
				: documentRepository.findAll();
		return documents.stream()
				.sorted(Comparator.comparing(AdministrativeDocument::getGeneratedAt).reversed())
				.map(this::toResponse)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<DocumentResponse> getMine() {
		Student student = currentStudent();
		return documentRepository.findByStudentId(student.getId()).stream()
				.sorted(Comparator.comparing(AdministrativeDocument::getGeneratedAt).reversed())
				.map(this::toResponse)
				.toList();
	}

	public DocumentResponse generate(GenerateDocumentRequest request) {
		requireAdministration();
		Student student = findStudent(request.studentId());
		byte[] pdf = switch (request.type()) {
			case ATTESTATION_SCOLARITE -> buildAttestation(student, request.academicYear());
			case CERTIFICAT_INSCRIPTION -> buildCertificat(student, request.academicYear());
			case RELEVE_NOTES -> buildReleveNotes(student, request.academicYear(), request.semester());
		};
		String suggestedName = request.type().name().toLowerCase() + "-" + student.getStudentIdentifier();
		String fileName = storageService.store(pdf, suggestedName);
		AdministrativeDocument document = new AdministrativeDocument(student, request.type(), request.academicYear(),
				request.semester(), fileName, currentUsername());
		return toResponse(documentRepository.save(document));
	}

	public record DownloadedFile(String fileName, byte[] content) {
	}

	@Transactional(readOnly = true)
	public DownloadedFile download(Long id) {
		AdministrativeDocument document = findDocument(id);
		if (!hasRole("ROLE_ADMINISTRATION")) {
			Student student = currentStudent();
			if (!document.getStudent().getId().equals(student.getId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot download this document");
			}
		}
		return new DownloadedFile(document.getFileName(), storageService.read(document.getFileName()));
	}

	public void delete(Long id) {
		requireAdministration();
		AdministrativeDocument document = findDocument(id);
		storageService.delete(document.getFileName());
		documentRepository.delete(document);
	}

	private byte[] buildAttestation(Student student, String academicYear) {
		String body = "Nous soussignés, " + institutionName + ", attestons que l'élève "
				+ student.getFirstName() + " " + student.getLastName()
				+ ", titulaire de la carte d'identité nationale n° " + student.getCin()
				+ " et portant l'identifiant " + student.getStudentIdentifier()
				+ ", est régulièrement inscrit(e) dans notre établissement au titre de l'année scolaire "
				+ academicYear + classSuffix(student) + ".\n\n"
				+ "La présente attestation est délivrée à l'intéressé(e) pour servir et valoir ce que de droit.";
		return buildLetter("ATTESTATION DE SCOLARITE", body);
	}

	private byte[] buildCertificat(Student student, String academicYear) {
		String body = "Nous soussignés, " + institutionName + ", certifions l'inscription de l'élève "
				+ student.getFirstName() + " " + student.getLastName()
				+ ", titulaire de la carte d'identité nationale n° " + student.getCin()
				+ ", au sein de notre établissement au titre de l'année scolaire " + academicYear
				+ classSuffix(student) + ".\n\n"
				+ "Le présent certificat est délivré à l'intéressé(e) pour servir et valoir ce que de droit.";
		return buildLetter("CERTIFICAT D'INSCRIPTION", body);
	}

	private String classSuffix(Student student) {
		SchoolClass schoolClass = student.getSchoolClass();
		if (schoolClass == null) {
			return "";
		}
		return ", en classe de " + schoolClass.getName() + " (" + schoolClass.getLevel().getName() + ")";
	}

	private byte[] buildLetter(String title, String body) {
		try (PdfDocumentBuilder pdf = new PdfDocumentBuilder()) {
			pdf.heading(institutionName)
					.italic(institutionAddress)
					.spacer(24)
					.title(title)
					.spacer(10)
					.keyValue("Fait le", LocalDate.now().format(DATE_FORMAT))
					.spacer(14)
					.paragraph(body)
					.spacer(40)
					.italic("L'Administration");
			return pdf.build();
		}
	}

	private byte[] buildReleveNotes(Student student, String academicYear, Semester semesterFilter) {
		List<Grade> grades = gradeRepository.findByStudentIdAndAcademicYear(student.getId(), academicYear).stream()
				.filter(grade -> semesterFilter == null || grade.getSemester() == semesterFilter)
				.sorted(Comparator.comparing(Grade::getSemester).thenComparing(Grade::getSubject))
				.toList();

		try (PdfDocumentBuilder pdf = new PdfDocumentBuilder()) {
			pdf.heading(institutionName)
					.italic(institutionAddress)
					.spacer(24)
					.title(semesterFilter != null ? "BULLETIN DE NOTES" : "RELEVE DE NOTES")
					.spacer(6)
					.keyValue("Eleve", student.getFirstName() + " " + student.getLastName())
					.keyValue("Identifiant", student.getStudentIdentifier())
					.keyValue("Annee scolaire", academicYear)
					.keyValue("Periode", semesterFilter != null ? semesterFilter.name() : "Annee complete (S1 + S2)")
					.spacer(16);

			float[] columns = {0, 260, 380};
			Map<Semester, List<Grade>> bySemester = grades.stream()
					.collect(Collectors.groupingBy(Grade::getSemester, () -> new EnumMap<>(Semester.class), Collectors.toList()));

			for (Semester semester : Semester.values()) {
				if (semesterFilter != null && semester != semesterFilter) {
					continue;
				}
				List<Grade> semesterGrades = bySemester.getOrDefault(semester, List.of());
				if (semesterGrades.isEmpty()) {
					continue;
				}
				pdf.heading("Semestre " + semester.name().substring(1));
				pdf.tableRow(new String[]{"Matiere", "Note /20", "Coefficient"}, columns, PdfDocumentBuilder.BOLD, 10.5f);
				pdf.rule();
				for (Grade grade : semesterGrades) {
					pdf.tableRow(new String[]{grade.getSubject(), format(grade.getScore()), format(grade.getCoefficient())},
							columns, PdfDocumentBuilder.REGULAR, 10.5f);
				}
				pdf.spacer(4);
				pdf.keyValue("Moyenne du semestre", format(weightedAverage(semesterGrades)) + "/20");
				pdf.spacer(14);
			}

			if (!grades.isEmpty()) {
				pdf.rule();
				pdf.keyValue("Moyenne generale", format(weightedAverage(grades)) + "/20");
			} else {
				pdf.paragraph("Aucune note enregistrée pour cette période.");
			}

			return pdf.build();
		}
	}

	private BigDecimal weightedAverage(List<Grade> grades) {
		BigDecimal weightedSum = BigDecimal.ZERO;
		BigDecimal coefficientSum = BigDecimal.ZERO;
		for (Grade grade : grades) {
			weightedSum = weightedSum.add(grade.getScore().multiply(grade.getCoefficient()));
			coefficientSum = coefficientSum.add(grade.getCoefficient());
		}
		if (coefficientSum.signum() == 0) {
			return BigDecimal.ZERO;
		}
		return weightedSum.divide(coefficientSum, 2, RoundingMode.HALF_UP);
	}

	private String format(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	private DocumentResponse toResponse(AdministrativeDocument document) {
		Student student = document.getStudent();
		return new DocumentResponse(
				document.getId(),
				student.getId(),
				student.getFirstName() + " " + student.getLastName(),
				student.getStudentIdentifier(),
				document.getType(),
				document.getAcademicYear(),
				document.getSemester(),
				document.getGeneratedAt(),
				document.getGeneratedBy());
	}

	private AdministrativeDocument findDocument(Long id) {
		return documentRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
	}

	private Student findStudent(Long id) {
		return studentRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
	}

	private Student currentStudent() {
		return studentRepository.findByEmailIgnoreCase(currentUsername())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student profile not found"));
	}

	private void requireAdministration() {
		if (!hasRole("ROLE_ADMINISTRATION")) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administration access required");
		}
	}

	private boolean hasRole(String role) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals(role));
	}

	private String currentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getName() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}
		return authentication.getName();
	}
}

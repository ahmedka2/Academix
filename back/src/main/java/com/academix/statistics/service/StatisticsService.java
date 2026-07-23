package com.academix.statistics.service;

import com.academix.absence.entity.Absence;
import com.academix.absence.entity.AbsenceStatus;
import com.academix.absence.repository.AbsenceRepository;
import com.academix.auth.entity.Role;
import com.academix.auth.repository.UserRepository;
import com.academix.classroom.entity.SchoolClass;
import com.academix.classroom.repository.SchoolClassRepository;
import com.academix.grade.entity.Grade;
import com.academix.grade.repository.GradeRepository;
import com.academix.invoice.entity.Invoice;
import com.academix.invoice.entity.InvoiceStatus;
import com.academix.invoice.repository.InvoiceRepository;
import com.academix.statistics.dto.AbsenceStatsResponse;
import com.academix.statistics.dto.CategoryCountResponse;
import com.academix.statistics.dto.ClassAverageResponse;
import com.academix.statistics.dto.PaymentStatsResponse;
import com.academix.statistics.dto.StatisticsResponse;
import com.academix.student.entity.Student;
import com.academix.student.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

	private final StudentRepository studentRepository;
	private final GradeRepository gradeRepository;
	private final AbsenceRepository absenceRepository;
	private final InvoiceRepository invoiceRepository;
	private final SchoolClassRepository schoolClassRepository;
	private final UserRepository userRepository;

	public StatisticsService(StudentRepository studentRepository, GradeRepository gradeRepository,
			AbsenceRepository absenceRepository, InvoiceRepository invoiceRepository,
			SchoolClassRepository schoolClassRepository, UserRepository userRepository) {
		this.studentRepository = studentRepository;
		this.gradeRepository = gradeRepository;
		this.absenceRepository = absenceRepository;
		this.invoiceRepository = invoiceRepository;
		this.schoolClassRepository = schoolClassRepository;
		this.userRepository = userRepository;
	}

	public StatisticsResponse getStatistics() {
		requireAdministration();

		List<Student> students = studentRepository.findAll();
		List<Grade> grades = gradeRepository.findAll();
		List<Absence> absences = absenceRepository.findAll();
		List<Invoice> invoices = invoiceRepository.findAll();
		List<SchoolClass> classes = schoolClassRepository.findAll();

		Map<Long, List<Grade>> gradesByStudent = grades.stream().collect(groupingBy(g -> g.getStudent().getId()));
		Map<Long, Double> studentAverages = new HashMap<>();
		for (Student student : students) {
			List<Grade> own = gradesByStudent.get(student.getId());
			if (own != null && !own.isEmpty()) {
				studentAverages.put(student.getId(), weightedAverage(own));
			}
		}

		long passCount = studentAverages.values().stream().filter(average -> average >= 10).count();
		double overallPassRate = studentAverages.isEmpty() ? 0 : round2((passCount * 100.0) / studentAverages.size());

		List<ClassAverageResponse> averageByClass = classes.stream()
				.map(schoolClass -> {
					List<Double> classAverages = students.stream()
							.filter(student -> student.getSchoolClass() != null && student.getSchoolClass().getId().equals(schoolClass.getId()))
							.map(student -> studentAverages.get(student.getId()))
							.filter(Objects::nonNull)
							.toList();
					double classAverage = classAverages.isEmpty() ? 0
							: round2(classAverages.stream().mapToDouble(Double::doubleValue).average().orElse(0));
					long studentCount = studentRepository.countBySchoolClassId(schoolClass.getId());
					return new ClassAverageResponse(schoolClass.getId(), schoolClass.getName(), schoolClass.getLevel().getName(),
							(int) studentCount, classAverage);
				})
				.sorted(Comparator.comparing(ClassAverageResponse::levelName).thenComparing(ClassAverageResponse::className))
				.toList();

		List<CategoryCountResponse> studentsByFieldOfStudy = countBy(students, Student::getFieldOfStudy);
		List<CategoryCountResponse> studentsByLevel = countBy(students,
				student -> student.getSchoolClass() == null ? "Unassigned" : student.getSchoolClass().getLevel().getName());

		AbsenceStatsResponse absenceStats = buildAbsenceStats(absences);
		PaymentStatsResponse paymentStats = buildPaymentStats(invoices);

		long totalTeachers = userRepository.findAllByRole(Role.TEACHER).size();

		return new StatisticsResponse(students.size(), totalTeachers, classes.size(), overallPassRate,
				averageByClass, studentsByFieldOfStudy, studentsByLevel, absenceStats, paymentStats);
	}

	private AbsenceStatsResponse buildAbsenceStats(List<Absence> absences) {
		long unjustified = absences.stream().filter(a -> a.getStatus() == AbsenceStatus.UNJUSTIFIED).count();
		long pending = absences.stream().filter(a -> a.getStatus() == AbsenceStatus.PENDING_JUSTIFICATION).count();
		long justified = absences.stream().filter(a -> a.getStatus() == AbsenceStatus.JUSTIFIED).count();
		long rejected = absences.stream().filter(a -> a.getStatus() == AbsenceStatus.REJECTED).count();
		double unjustifiedRate = absences.isEmpty() ? 0 : round2((unjustified * 100.0) / absences.size());
		return new AbsenceStatsResponse(absences.size(), unjustified, pending, justified, rejected, unjustifiedRate);
	}

	private PaymentStatsResponse buildPaymentStats(List<Invoice> invoices) {
		long paidCount = invoices.stream().filter(i -> i.getStatus() == InvoiceStatus.PAID).count();
		long unpaidCount = invoices.size() - paidCount;
		BigDecimal totalAmount = invoices.stream().map(Invoice::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal paidAmount = invoices.stream()
				.filter(i -> i.getStatus() == InvoiceStatus.PAID)
				.map(Invoice::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal outstandingAmount = totalAmount.subtract(paidAmount);
		return new PaymentStatsResponse(invoices.size(), paidCount, unpaidCount, totalAmount, paidAmount, outstandingAmount);
	}

	private List<CategoryCountResponse> countBy(List<Student> students, java.util.function.Function<Student, String> classifier) {
		return students.stream()
				.collect(groupingBy(classifier, counting()))
				.entrySet().stream()
				.map(entry -> new CategoryCountResponse(entry.getKey(), entry.getValue()))
				.sorted(Comparator.comparing(CategoryCountResponse::count).reversed())
				.toList();
	}

	private double weightedAverage(List<Grade> grades) {
		BigDecimal totalCoefficient = grades.stream().map(Grade::getCoefficient).reduce(BigDecimal.ZERO, BigDecimal::add);
		if (totalCoefficient.signum() == 0) {
			return 0;
		}
		BigDecimal weightedSum = grades.stream()
				.map(grade -> grade.getScore().multiply(grade.getCoefficient()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return weightedSum.divide(totalCoefficient, 4, RoundingMode.HALF_UP).doubleValue();
	}

	private double round2(double value) {
		return Math.round(value * 100.0) / 100.0;
	}

	private void requireAdministration() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRATION"));
		if (!isAdmin) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administration access required");
		}
	}
}

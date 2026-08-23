import { Routes } from '@angular/router';

import { AbsencesComponent } from './absences/absences.component';
import { LoginComponent } from './auth/login.component';
import { ClassesComponent } from './classes/classes.component';
import { MyClassesComponent } from './classes/my-classes.component';
import { authGuard, guestGuard, roleGuard } from './core/auth.guard';
import { DocumentsComponent } from './documents/documents.component';
import { GradesComponent } from './grades/grades.component';
import { InvoicesComponent } from './invoices/invoices.component';
import { ShellComponent } from './layout/shell.component';
import { ProfileComponent } from './profile/profile.component';
import { StageDocumentsComponent } from './stage-documents/stage-documents.component';
import { StatisticsComponent } from './statistics/statistics.component';
import { StudentsComponent } from './students/students.component';
import { TeachersComponent } from './teachers/teachers.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'students', component: StudentsComponent, canActivate: [roleGuard(['ADMINISTRATION', 'TEACHER'])] },
      { path: 'teachers', component: TeachersComponent, canActivate: [roleGuard(['ADMINISTRATION'])] },
      { path: 'classes', component: ClassesComponent, canActivate: [roleGuard(['ADMINISTRATION'])] },
      { path: 'my-classes', component: MyClassesComponent, canActivate: [roleGuard(['TEACHER'])] },
      { path: 'invoices', component: InvoicesComponent, canActivate: [roleGuard(['ADMINISTRATION'])] },
      { path: 'grades', component: GradesComponent, canActivate: [roleGuard(['ADMINISTRATION', 'TEACHER'])] },
      { path: 'absences', component: AbsencesComponent, canActivate: [roleGuard(['ADMINISTRATION', 'TEACHER'])] },
      { path: 'statistics', component: StatisticsComponent, canActivate: [roleGuard(['ADMINISTRATION'])] },
      { path: 'documents', component: DocumentsComponent, canActivate: [roleGuard(['ADMINISTRATION'])] },
      { path: 'stage-documents', component: StageDocumentsComponent, canActivate: [roleGuard(['ADMINISTRATION'])] },
      { path: 'me', component: ProfileComponent, canActivate: [roleGuard(['STUDENT'])] },
      { path: '', redirectTo: 'students', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'students' }
];

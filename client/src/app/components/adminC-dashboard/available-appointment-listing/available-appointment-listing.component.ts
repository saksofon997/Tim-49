import { Component, OnInit } from '@angular/core';
import { NavigationEnd, Router, ActivatedRoute } from '@angular/router';
import { ClinicService } from 'src/app/services/clinic.service';
import { UserService } from 'src/app/services/user.service';
import { AppointmentService } from 'src/app/services/appointment.service';
import { DialogService } from 'src/app/services/dialog.service';

@Component({
  selector: 'app-available-appointment-listing',
  templateUrl: './available-appointment-listing.component.html',
  styleUrls: ['./available-appointment-listing.component.css']
})
export class AvailableAppointmentListingComponent implements OnInit {
	appointmentHeaders = ['Doctor', 'Ordination name and number', 'Type', 'Date and time', 'Duration (minutes)', 'Price', 'Discount'];
	appointments: any;
	navigationSubscription: any;
	clinicID: any;
	clinicName: any;
	constructor(private router: Router,
		private activatedRoute: ActivatedRoute,
		private userService: UserService,
		private appointmentService: AppointmentService,
		private confirmationDialogService: DialogService
	) {
		this.navigationSubscription = this.router.events.subscribe((e: any) => {
			if (e instanceof NavigationEnd) {
				this.clinicID = this.userService.getUser().clinic_id;
				this.getAppointments();
			}
		});
	}

	ngOnInit() {
		this.activatedRoute.params.subscribe((params) => {
			this.clinicID = params.id;
			this.clinicName = params.name;
		});
	}
	getAppointments() {
		this.clinicID = this.userService.getUser().clinic_id;
		this.appointmentService.getClinicAvailableAppointments(this.clinicID).subscribe(
			(data) => {
				this.appointments = data;
				for (const [i, value] of this.appointments.entries()) {
					this.appointments[i].formatedDateAndTime = this.timeConverter(this.appointments[i].startingDateAndTime);
				}
				this.appointments.sort((a, b) => (a.id > b.id) ? 1 : -1)
			},
			(error) => {
				alert(error);
			}
		);
	}

	addNewAppointment() {
		this.router.navigate(['../new_available_appointment'], { relativeTo: this.activatedRoute });
	}

	deleteAppointment(appointment) {
		this.confirmationDialogService.confirm('Please confirm', 'Are you sure you want to delete available appointment: ' + appointment.doctors[0].name+' '+appointment.doctors[0].surname+' '+appointment.ordination.name  + '?', false)
			.then((confirmed) => {
				if (confirmed.submited) {
					this.appointmentService.deleteAvailableAppointment(appointment.id).subscribe(
						(data) => {
							this.getAppointments();
						},
						(error) => {
							alert(error);
						}
					)
				}
			});
	}

	timeConverter(UNIX_timestamp) {
		var a = new Date(UNIX_timestamp * 1000);
		var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
		var year = a.getFullYear();
		var month = months[a.getMonth()];
		var date = a.getDate();
		var hour = a.getHours();
		var min = a.getMinutes() < 10 ? '0' + a.getMinutes() : a.getMinutes();
		var sec = a.getSeconds() < 10 ? '0' + a.getSeconds() : a.getSeconds();
		var time = date + '. ' + month + ' ' + year + '. ' + hour + ':' + min + ':' + sec;
		return time;
	}

	ngOnDestroy() {
		if (this.navigationSubscription) {
			this.navigationSubscription.unsubscribe();
		}
	}
}

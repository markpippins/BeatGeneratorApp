import {Component} from '@angular/core';
// import {Component, Inject} from '@angular/core';
// import {MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";

@Component({
  selector: 'app-instrument-select-dialog',
  templateUrl: './instrument-select-dialog.component.html',
  styleUrls: ['./instrument-select-dialog.component.css']
})
export class InstrumentSelectDialogComponent {
  message: string = ""
  cancelButtonText = "Cancel"
  // constructor(
    // @Inject(MAT_DIALOG_DATA) private _data: any,
    // private dialogRef: MatDialogRef<InstrumentSelectDialogComponent>) {
    // if (_data) {
    //   this.message = _data.message || this.message;
    //   if (_data.buttonText) {
    //     this.cancelButtonText = _data.buttonText.cancel || this.cancelButtonText;
    //   }
    // }
    // this.dialogRef.updateSize('300vw','300vw')
  // }

  // onConfirmClick(): void {
  //   this.dialogRef.close(true);
  // }
}

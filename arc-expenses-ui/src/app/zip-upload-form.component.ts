/**
 * Created by stefania on 1/20/17.
 */
import { Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { ResourceService } from "../../../services/resource.service";
import { Resource } from "../../../domain/resource";

declare var UIkit: any;

@Component({
    selector: 'zip-upload',
    template: `
    <form [formGroup]="myForm" class="zipUpload">
        <div data-uk-grid-margin="" class="uk-grid uk-grid-width-1-1">
            <div class="form-group uk-width-1-1">
                <div class="js-upload uk-placeholder uk-text-center">
                    <span uk-icon="icon: cloud-upload"></span>
                    <span class="uk-text-middle">Attach your zip file by dropping it here or by</span>
                    <div uk-form-custom>
                        <input type="file" multiple name="xmlFile" (change)="report($event)" formControlName="fileUpload">
                        <span class="uk-link">selecting one</span>
                    </div>
                    <div>&lt; {{uploadedZip ? uploadedZip.name : "No file selected"}} &gt;</div>
                </div>
                <!--<input type="file" name="xmlFile" (change)="report($event)" formControlName="fileUpload">-->
            </div>
        </div>
    </form>
    <!--<progress #progressBar id="js-progressbar" class="uk-progress" value="0" max="100" hidden></progress>-->
    <!--<a *ngIf="clicks < clickMe.length" (click)="onSubmit(myForm)">{{clickMe[clicks % clickMe.length]}}</a>-->
    <!--<a *ngIf="clicks == clickMe.length" href="https://cdn.meme.am/cache/instances/folder598/52560598.jpg">{{clickMe[clicks % clickMe.length]}}</a>-->
    `,
    styles: [`
        .zipUpload input {
          height: 35px;
        }
    `],
})

export class ZipUploadComponent implements OnInit{

    // private clickMe : string[] = [
    //     "click me baby one more time","yeah, click me again", "don't stop I like it", "please baby, please click me"
    // ]
    // private clicks = 0;

    @Input('group')
    myForm: FormGroup;

    @Output('file')
    fileChange : EventEmitter<File> = new EventEmitter<File>();

    errorMessage: string;
    successMessage: string;

    uploadedZip : File;

    // @ViewChild('progressBar')
    // progressBar : ElementRef;

    //private resource: Resource;

    constructor(private _fb: FormBuilder, private router: Router, private activatedRoute: ActivatedRoute,
                resourceService: ResourceService) {

        this.uploadedZip = null;
    }

    ngOnInit() {
        this.myForm = this._fb.group({
            fileUpload: ''
        });

        //var bar = document.getElementById('js-progressbar');
        // let bar = this.progressBar.nativeElement;
        UIkit.upload('.js-upload', {

            url: '',
            multiple: true,

            beforeSend: function () {
                console.log('beforeSend', arguments);
            },
            beforeAll: function () {
                console.log('beforeAll', arguments);
            },
            load: function () {
                console.log('load', arguments);
            },
            error: function () {
                console.log('error', arguments);
            },
            complete: function () {
                console.log('complete', arguments);
            },

            loadStart: function (e) {
                console.log('loadStart', arguments);

                // bar.removeAttribute('hidden');
                // bar.setAttribute("width", "0%");
                // bar.max = e.total;
                // bar.value = e.loaded;
            },

            progress: function (e) {
                console.log('progress', arguments);
                // var percent = Math.ceil(percent);
                // bar.setAttribute("width", percent +"%");
                // bar.max = e.total;
                // bar.value = e.loaded;
            },

            loadEnd: function (e) {
                console.log('loadEnd', arguments);
                // bar.setAttribute("width", "100%");

                // bar.max = e.total;
                // bar.value = e.loaded;
            },

            completeAll: function (e) {
                console.log('completeAll', arguments);

                // setTimeout(function () {
                //     bar.setAttribute('hidden', 'hidden');
                // }, 10000);
                //
                // alert('Upload Completed');
            }

        });
    }

    onSubmit(myForm: FormGroup, event: Event) {
        // this.clicks++;
        console.log("zipValue",myForm.value);

        // event.preventDefault();
        this.previewFromFile();
        // this.successMessage = null;
        // console.log(componentXML.xml);
        //
        // var resource: Resource = new Resource();
        // resource.payload = componentXML.xml;
        // resource.resourceType = 'component';
        // resource.payloadFormat = 'xml';
        //
        // this.resourceService.registerComponent(resource).subscribe(
        //     resource => this.successfullySubscribed(resource),
        //     error => this.handleError(<any>error));
    }

    previewFromFile() {
        if (this.uploadedZip) {
            var myReader: FileReader = new FileReader();
            //var tempForm = this.componentXMLForm;
            console.log(this.uploadedZip);
            const self = this;
            myReader.onloadstart = function (e) {
                if (!self.uploadedZip.name.endsWith(".zip")) {
                    throw "Not a zip file";
                }
            };
            myReader.onloadend = function (e) {
                console.log(myReader.result)
                // tempForm.setValue({'xml' : myReader.result});
            };
            myReader.readAsText(this.uploadedZip);
        }
    }
    report($event : any) {
        this.uploadedZip = $event.target.files[0];
        console.log(this.uploadedZip);
        this.fileChange.emit(this.uploadedZip);
    }

    successfullySubscribed(resource : Resource) {
        this.successMessage = 'Your component has been successfully registered';
        return false;
    }

    handleError(error) {
        this.errorMessage = 'System error registering your component (Server responded: ' + error + ')';
    }
}
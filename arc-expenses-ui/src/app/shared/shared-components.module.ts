import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AsideHelpContentComponent, HelpContentComponent } from './help-content/help-content.component';

@NgModule({
    imports: [
        CommonModule,
    ],
    declarations: [
        HelpContentComponent,
        AsideHelpContentComponent
    ],
    exports: [
        HelpContentComponent,
        AsideHelpContentComponent
    ]
})
export class SharedComponentsModule {}

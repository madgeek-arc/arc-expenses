import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AsideHelpContentComponent, HelpContentComponent } from './help-content/help-content.component';
import { ReadMoreComponent } from './read-more/read-more.component';

@NgModule({
    imports: [
        CommonModule,
    ],
    declarations: [
        HelpContentComponent,
        AsideHelpContentComponent,
        ReadMoreComponent
    ],
    exports: [
        HelpContentComponent,
        AsideHelpContentComponent,
        ReadMoreComponent
    ]
})
export class SharedComponentsModule {}

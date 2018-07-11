import { Type } from '@angular/core';

export class StageItem {
    constructor(public component: Type<any>, public data: any) {}
}
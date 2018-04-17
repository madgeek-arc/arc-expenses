// Generated using typescript-generator version 2.1.406 on 2018-04-17 09:50:02.

export class Attachment {
    filename: string;
    mimetype: string;
    size: number;
    url: any;
}

export class Delegate {
    email: string;
    firstname: string;
    lastname: string;
    hidden: boolean;
}

export class Institute {
    id: string;
    name: string;
    organization: Organization;
    director: POY;
    accountingRegistration: POY;
    accountingPayment: POY;
    accountingDirector: POY;
    diaugeia: POY;
}

export class Organization {
    id: string;
    name: string;
    POY: POY;
    director: POY;
    dioikitikoSumvoulio: POY;
}

export class POY {
    email: string;
    firstname: string;
    lastname: string;
    delegates: Delegate[];
}

export class Project {
    id: string;
    name: string;
    acronym: string;
    institute: Institute;
    parentProject: Project;
    scientificCoordinator: POY;
    operator: POY[];
    startDate: any;
    endDate: any;
}

export class Request {
    id: string;
    project: Project;
    requester: Requester;
    requesterPosition: string;
    stage1: Stage1;
    stage2: Stage2;
    stage3: Stage3;
    stage3a: Stage3a;
    stage3b: Stage3b;
    stage4: Stage4;
    stage5: Stage5;
    stage6: Stage6;
    stage7: Stage7;
    stage8: Stage8;
    stage9: Stage9;
    stage10: Stage10;
}

export class Requester {
    id: string;
    email: string;
    firstname: string;
    lastname: string;
    firstnameLatin: string;
    lastnameLatin: string;
}

export class Stage1 {
    requestDate: string;
    subject: string;
    supplier: string;
    supplierSelectionMethod: string;
    amountInEuros: number;
    attachment: Attachment;
}

export class Stage10 {
    accountingPayment: Delegate;
    date: any;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage2 {
    scientificCoordinator: Delegate;
    date: any;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage3 {
    operator: Delegate;
    date: any;
    analiftheiYpoxrewsi: boolean;
    fundsAvailable: boolean;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage3a {
    date: any;
    organizationDirector: Delegate;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage3b {
    date: any;
    dioikitikoSumvoulio: Delegate;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage4 {
    POY: Delegate;
    date: any;
    analiftheiYpoxrewsi: boolean;
    fundsAvailable: boolean;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage5 {
    instituteDirector: Delegate;
    date: any;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage6 {
    organizationDiaugeia: Delegate;
    date: any;
    comment: string;
    attachment: Attachment;
}

export class Stage7 {
    accountingDirector: Delegate;
    date: any;
    checkRegularity: boolean;
    checkLegality: boolean;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage8 {
    POY: Delegate;
    date: any;
    checkRegularity: boolean;
    checkLegality: boolean;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage9 {
    accountingRegistration: Delegate;
    date: any;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

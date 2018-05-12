// Generated using typescript-generator version 2.1.406 on 2018-04-24 15:14:12.

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
/*    parentProject: Project;*/
    scientificCoordinator: POY;
    operator: POY[];
    startDate: string;
    endDate: string;
}

export class Request {
    id: string;
    project: Project;
    requester: Requester;
    requesterPosition: string;
    stage: string;
    status: any;
    stage1: Stage1;
    stage2: Stage2;
    stage3: Stage3;
    stage4: Stage4;
    stage5: Stage5;
    stage5a: Stage5a;
    stage5b: Stage5b;
    stage6: Stage6;
    stage7: Stage7;
    stage8: Stage8;
    stage9: Stage9;
    stage10: Stage10;
    stage11: Stage11;
    stage12: Stage12;
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

export class Stage2 {
    scientificCoordinator: Delegate;
    date: string;
    approved: boolean;
    isNecessary: boolean;
    isAdvisable: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage3 {
    operator: Delegate;
    date: string;
    analiftheiYpoxrewsi: boolean;
    fundsAvailable: boolean;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage4 {
    POY: Delegate;
    date: string;
    analiftheiYpoxrewsi: boolean;
    fundsAvailable: boolean;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage5 {
    instituteDirector: Delegate;
    date: string;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage5a {
    date: string;
    organizationDirector: Delegate;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage5b {
    date: string;
    dioikitikoSumvoulio: Delegate;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage6 {
    organizationDiaugeia: Delegate;
    date: string;
    comment: string;
    attachment: Attachment;
}

export class Stage7 {
    operator: Delegate;
    date: string;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage8 {
    accountingDirector: Delegate;
    date: string;
    checkRegularity: boolean;
    checkLegality: boolean;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage9 {
    POY: Delegate;
    date: string;
    checkRegularity: boolean;
    checkLegality: boolean;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage10 {
    accountingRegistration: Delegate;
    date: string;
    approved: boolean;
    accountingCode: string;
    accountingDescription: string;
    comment: string;
    attachment: Attachment;
}

export class Stage11 {
    organizationDiaugeia: Delegate;
    date: string;
    comment: string;
    attachment: Attachment;
}

export class Stage12 {
    accountingPayment: Delegate;
    date: string;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

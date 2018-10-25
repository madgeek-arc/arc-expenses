// Generated using typescript-generator version 2.1.406 on 2018-10-23 13:46:16.

export class RequestFatClass {
    request_id: string;
    id: string;
    user: User;
    project: Project;
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
    stage13: Stage13;
}

export class Vocabulary {
    projectID: string;
    projectAcronym: string;
    instituteName: string;
}

export class Attachment {
    filename: string;
    mimetype: string;
    size: number;
    url: string;
}

export class BaseInfo {
    id: string;
    requestId: string;
    creationDate: string;
    stage: string;
    status: string;
}

export class ContactUsMail {
    name: string;
    surname: string;
    email: string;
    subject: string;
    message: string;
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
    director: PersonOfInterest;
    accountingRegistration: PersonOfInterest;
    accountingPayment: PersonOfInterest;
    accountingDirector: PersonOfInterest;
    diaugeia: PersonOfInterest;
    suppliesOffice: PersonOfInterest;
    travelManager: PersonOfInterest;
    diataktis: PersonOfInterest;}

export class Organization {
    id: string;
    name: string;
    poy: PersonOfInterest;
    director: PersonOfInterest;
    viceDirector: PersonOfInterest;
    inspectionTeam: PersonOfInterest[];
    dioikitikoSumvoulio: PersonOfInterest;
}

export class PersonOfInterest {
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
    parentProject: string;
    scientificCoordinator: PersonOfInterest;
    operator: PersonOfInterest[];
    startDate: string;
    endDate: string;
    totalCost: number;
}

export class Request {
    stage1: Stage1;
    id: string;
    type: string;
    archiveId: string;
    project: Project;
    user: User;
    requesterPosition: string;
    requestStatus: string;
    trip: Trip;
    scientificCoordinatorAsDiataktis: boolean;
}

export class RequestApproval extends BaseInfo {
    stage2: Stage2;
    stage3: Stage3;
    stage4: Stage4;
    stage5: Stage5;
    stage5a: Stage5a;
    stage5b: Stage5b;
    stage6: Stage6;
}

export class RequestPayment extends BaseInfo {
    stage7: Stage7;
    stage8: Stage8;
    stage9: Stage9;
    stage10: Stage10;
    stage11: Stage11;
    stage12: Stage12;
    stage13: Stage13;
}

export class RequestSummary {
    baseInfo: BaseInfo;
    request: Request;
}

export class Stage1 {
    requestDate: string;
    subject: string;
    supplier: string;
    supplierSelectionMethod: string;
    amountInEuros: number;
    finalAmount: number;
    attachment: Attachment;
}

export class Stage2 {
    user: User;
    date: string;
    checkFeasibility: boolean;
    checkNecessity: boolean;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage3 {
    user: User;
    date: string;
    analiftheiYpoxrewsi: boolean;
    fundsAvailable: boolean;
    loan: boolean;
    loanSource: string;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage4 {
    user: User;
    date: string;
    analiftheiYpoxrewsi: boolean;
    fundsAvailable: boolean;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage5 {
    user: User;
    date: string;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage5a {
    user: User;
    date: string;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage5b {
    user: User;
    date: string;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage6 {
    user: User;
    date: string;
    comment: string;
    attachment: Attachment;
}

export class Stage7 {
    user: User;
    date: string;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage8 {
    user: User;
    date: string;
    checkRegularity: boolean;
    checkLegality: boolean;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage9 {
    user: User;
    date: string;
    checkRegularity: boolean;
    checkLegality: boolean;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage10 {
    user: User;
    date: string;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage11 {
    user: User;
    date: string;
    comment: string;
    attachment: Attachment;
}

export class Stage12 {
    user: User;
    date: string;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Stage13 {
    user: User;
    date: string;
    approved: boolean;
    comment: string;
    attachment: Attachment;
}

export class Trip {
    firstname: string;
    lastname: string;
    email: string;
    destination: string;
}

export class User {
    id: string;
    email: string;
    firstname: string;
    lastname: string;
    firstnameLatin: string;
    lastnameLatin: string;
    signatureArchiveId: string;
    receiveEmails: string;
    immediateEmails: string;
    attachment: Attachment;
}

export class SignatureAttachment {
    filename: string;
    mimetype: string;
    size: number;
    url: string;
}


import {
    analiftheiYpoxrewsiDesc,
    checkFeasibilityDesc, checkLegalityDesc,
    checkNecessityDesc, checkRegularityDesc,
    commentDesc,
    fundsAvailableDesc, loanDesc, loanSourceDesc,
    StageFieldDescription, stageTitles
} from './stageDescriptions';
import {Type} from '@angular/core';
import {POI, Project, User} from './operation';
import {
    Stage10Component, Stage11Component, Stage12Component, Stage13Component,
    Stage2Component,
    Stage3Component,
    Stage4Component,
    Stage5aComponent,
    Stage5bComponent,
    Stage6Component, Stage7Component, Stage8Component, Stage9Component
} from '../request-stage/stages-components';


/* Map of the displayed messages according to the stage's outcome (if submitted) */
export class SubmittedStageResultMap {
    '2': string;    // message if the stage was approved
    '3': string;    // message if the stage was rejected
    '4': string;    // message if the stage was returned to the previous one

    constructor(str2: string, str3: string, str4: string) {
        this['2'] = str2;
        this['3'] = str3;
        this['4'] = str4;
    }
}

/* Information describing a stage */
export class StageInfo {
    title: string;                                      // a title
    prev: string[];                                     // list of the possible ids of the previous stage
    next: string[];                                     // list of the possible ids of the next stage
    stageComponent: Type<any>;                          // the stage-component that corresponds to the stage
    stageFields: StageFieldDescription [];              // a list of the stage's fields descriptions
    stagePOIs: POI[];                                   // a list of possible POIs for the stage
    submittedStageResultMap: SubmittedStageResultMap;   // a map with descriptions of the stage's result (if submitted)
    showStage: number;                                  // an indicator of the stage's display status

    constructor(title: string,
                prev: string[],
                next: string[],
                stageComponent: Type<any>,
                stageFields: StageFieldDescription [],
                stagePOIs: POI[],
                submittedStageResultMap: SubmittedStageResultMap) {

        this.title = title;
        this.prev = prev;
        this.next = next;
        this.stageComponent = stageComponent;
        this.stageFields = stageFields;
        this.stagePOIs = stagePOIs;
        this.submittedStageResultMap = submittedStageResultMap;
    }
}

export class RequestInfo {

    phaseId: string;
    requestId: string;
    pendingStageId: string;

    requestedAmount: string;
    supplier: string;
    requester: User;

    '2': StageInfo;
    '3': StageInfo;
    '4': StageInfo;
    '5': StageInfo;
    '5a': StageInfo;
    '5b': StageInfo;
    '6': StageInfo;
    '7': StageInfo;
    '8': StageInfo;
    '9': StageInfo;
    '10': StageInfo;
    '11': StageInfo;
    '12': StageInfo;
    '13': StageInfo;

    constructor(phaseId: string, requestId: string, project: Project) {

        this.requestId = requestId;
        this.initiateStagesInfo(project);
    }

    initiateStagesInfo(project: Project) {
        this['2'] = new StageInfo(
            stageTitles['2'],
            ['1'],
            ['3'],
            Stage2Component,
            [checkNecessityDesc, checkFeasibilityDesc, commentDesc],
            [project.scientificCoordinator],
            new SubmittedStageResultMap(
                'Εγκρίθηκε από τον επιστημονικό υπεύθυνο',
                'Απορρίφθηκε από τον επιστημονικό υπεύθυνο',
                'Επεστράφη στο προηγούμενο στάδιο από τον επιστημονικό υπεύθυνο'
            )
        );

        this['3'] = new StageInfo(
            stageTitles['3'],
            ['2'],
            ['4'],
            Stage3Component,
            [analiftheiYpoxrewsiDesc, fundsAvailableDesc, loanDesc, loanSourceDesc, commentDesc],
            project.operator,
            new SubmittedStageResultMap(
                'Εγκρίθηκε από τον χειριστή του προγράμματος',
                'Απορρίφθηκε από τον χειριστή του προγράμματος',
                'Επεστράφη στο προηγούμενο στάδιο από τον χειριστή του προγράμματος'
            )
        );

        this['4'] = new StageInfo(
            stageTitles['4'],
            ['3'],
            ['5a'],
            Stage4Component,
            [analiftheiYpoxrewsiDesc, fundsAvailableDesc, commentDesc],
            [project.institute.organization.POI],
            new SubmittedStageResultMap(
                'Εγκρίθηκε από τον Προϊστάμενο Οικονομικών Υπηρεσιών',
                'Απορρίφθηκε από τον Προϊστάμενο Οικονομικών Υπηρεσιών',
                'Επεστράφη στο προηγούμενο στάδιο από τον Προϊστάμενο Οικονομικών Υπηρεσιών'
            )
        );

        this['5a'] = new StageInfo(
            stageTitles['5a'],
            ['4'],
            ['5b', '6'],
            Stage5aComponent,
            [commentDesc],
            [project.institute.director], // used to be organization.director
            new SubmittedStageResultMap(
                'Εγκρίθηκε από τον Διατάκτη',
                'Απορρίφθηκε από τον Διατάκτη',
                'Επεστράφη στο προηγούμενο στάδιο από τον Διατάκτη'
            )
        );

        this['5b'] = new StageInfo(
            stageTitles['5b'],
            ['5a'],
            ['6'],
            Stage5bComponent,
            [commentDesc],
            [project.institute.organization.dioikitikoSumvoulio],
            new SubmittedStageResultMap(
                'Εγκρίθηκε από το Διοικητικό Συμβούλιο',
                'Απορρίφθηκε από το Διοικητικό Συμβούλιο',
                'Επεστράφη στο προηγούμενο στάδιο από το Διοικητικό Συμβούλιο'
            )
        );

        this['6'] = new StageInfo(
            stageTitles['6'],
            ['5b', '5a'],
            [],
            Stage6Component,
            [commentDesc],
            [project.institute.diaugeia],
            new SubmittedStageResultMap(
                'Αναρτήθηκε στην ΔΙΑΥΓΕΙΑ',
                'Απορρίφθηκε πριν αναρτηθεί στην ΔΙΑΥΓΕΙΑ',
                'Επεστράφη στο προηγούμενο στάδιο πριν αναρτηθεί στην ΔΙΑΥΓΕΙΑ'
            )
        );

        this['7'] = new StageInfo(
            stageTitles['7'],
            [],
            ['8'],
            Stage7Component,
            [commentDesc],
            [project.institute.suppliesOffice],
            new SubmittedStageResultMap(
                'Εγκρίθηκε από το Γραφείο Προμηθειών',
                'Απορρίφθηκε από το Γραφείο Προμηθειών',
                'Επεστράφη στο προηγούμενο στάδιο από το Γραφείο Προμηθειών'
            )
        );

        this['8'] = new StageInfo(
            stageTitles['8'],
            ['7'],
            ['9'],
            Stage8Component,
            [checkRegularityDesc, checkLegalityDesc, commentDesc],
            project.institute.organization.inspectionTeam,
            new SubmittedStageResultMap(
                'Εγκρίθηκε από την Ομάδα Ελέγχου',
                'Απορρίφθηκε από την Ομάδα Ελέγχου',
                'Επεστράφη στο προηγούμενο στάδιο από την Ομάδα Ελέγχου'
            )
        );

        this['9'] = new StageInfo(
            stageTitles['9'],
            ['8'],
            ['10'],
            Stage9Component,
            [checkRegularityDesc, checkLegalityDesc, commentDesc],
            [project.institute.organization.POI],
            new SubmittedStageResultMap(
                'Εγκρίθηκε από τον Προϊστάμενο Οικονομικών Υπηρεσιών',
                'Απορρίφθηκε από τον Προϊστάμενο Οικονομικών Υπηρεσιών',
                'Επεστράφη στο προηγούμενο στάδιο από τον Προϊστάμενο Οικονομικών Υπηρεσιών'
            )
        );

        this['10'] = new StageInfo(
            stageTitles['10'],
            ['9'],
            ['11'],
            Stage10Component,
            [commentDesc],
            [project.institute.director], // used to be organization.director
            new SubmittedStageResultMap(
                'Εγκρίθηκε από τον Διατάκτη',
                'Απορρίφθηκε από τον Διατάκτη',
                'Επεστράφη στο προηγούμενο στάδιο από τον Διατάκτη'
            )
        );

        this['11'] = new StageInfo(
            stageTitles['11'],
            ['10'],
            ['12'],
            Stage11Component,
            [commentDesc],
            [project.institute.diaugeia],
            new SubmittedStageResultMap(
                'Αναρτήθηκε στην ΔΙΑΥΓΕΙΑ',
                'Απορρίφθηκε πριν αναρτηθεί στην ΔΙΑΥΓΕΙΑ',
                'Επεστράφη στο προηγούμενο στάδιο πριν αναρτηθεί στην ΔΙΑΥΓΕΙΑ'
            )
        );

        this['12'] = new StageInfo(
            stageTitles['12'],
            ['11'],
            ['13'],
            Stage12Component,
            [commentDesc],
            [project.institute.accountingRegistration],
            new SubmittedStageResultMap(
                'Εγκρίθηκε',
                'Απορρίφθηκε',
                'Επεστράφη στο προηγούμενο στάδιο'
            )
        );

        this['13'] = new StageInfo(
            stageTitles['13'],
            ['12'],
            [],
            Stage13Component,
            [commentDesc],
            [project.institute.accountingPayment],
            new SubmittedStageResultMap(
                'Εγκρίθηκε',
                'Απορρίφθηκε',
                'Επεστράφη στο προηγούμενο στάδιο'
            )
        );

    }

    // findIfUserIsPOI()

}

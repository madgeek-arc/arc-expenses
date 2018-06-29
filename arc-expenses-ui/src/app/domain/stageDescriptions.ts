/* shared constants */

/* NEXT TIME WE EMPTY THE DB REMEMBER TO:
    CHANGE 'regular' TO 'supply' AND USE supplierSelectionMethod terms in english
    CHANGE 'accepted' status TO 'completed'
    ADD english terms as values in supplierSelectionMethops (see supplierSelectionMethodsMap) */
import {Type} from "@angular/core";
import {
    Stage10Component,
    Stage11Component,
    Stage12Component,
    Stage13Component, Stage2Component, Stage3Component, Stage4Component,
    Stage5aComponent,
    Stage5bComponent,
    Stage6Component,
    Stage7Component,
    Stage8Component,
    Stage9Component
} from "../request-stage/stages-components";

export const requestTypes = {regular: 'Προμήθεια', trip: 'Ταξίδι', contract: 'Σύμβαση Έργου', services_contract: 'Σύμβαση Υπηρεσίας'};
export const statesList = ['all', 'accepted', 'pending', 'under_review', 'rejected'];
/*export const statesList = ['all', 'accepted', 'pending', 'rejected'];*/
export const supplierSelectionMethods = ['Απ\' ευθείας ανάθεση', 'Έρευνα αγοράς', 'Διαγωνισμός'];
export const supplierSelectionMethodsMap = {
    direct: 'Απ\' ευθείας ανάθεση', market_investigation: 'Έρευνα αγοράς', competition: 'Διαγωνισμός' };

/* the descriptions are used for dynamically loading request update forms and viewing results per stage */
export class StageFieldDescription {
    id: string;
    label: string;
    type: string;
    description: string;
    required: boolean;
}

export const commentDesc = {
    id: 'comment',
    label: 'Σχόλια',
    type: 'textarea',
    description: '',
    required: true
};

export const fundsAvailableDesc = {
    id: 'fundsAvailable',
    label: 'Υπάρχει διαθέσιμη πίστωση',
    type: 'checkbox',
    description: '',
    required: true
};

export const analiftheiYpoxrewsiDesc = {
    id: 'analiftheiYpoxrewsi',
    label: 'Έχει αναληφθεί η υποχρέωση',
    type: 'checkbox',
    description: '',
    required: true
};

export const checkRegularityDesc = {
    id: 'checkRegularity',
    label: 'Έγινε έλεγχος κανονικότητας',
    type: 'checkbox',
    description: '',
    required: true
};

export const checkLegalityDesc = {
    id: 'checkLegality',
    label: 'Έγινε έλεγχος νομιμότητας',
    type: 'checkbox',
    description: '',
    required: true
};

export const checkNecessityDesc = {
    id: 'checkNecessity',
    label: 'Έγινε έλεγχος αναγκαιότητας',
    type: 'checkbox',
    description: '',
    required: true
};

export const checkFeasibilityDesc = {
    id: 'checkFeasibility',
    label: 'Έγινε έλεγχος σκοπιμότητας',
    type: 'checkbox',
    description: '',
    required: true
};

export  const loanDesc = {
    id: 'loan',
    label: 'Έγινε δανεισμός',
    type: 'checkbox',
    description: '',
    required: true
};

export  const loanSourceDesc = {
    id: 'loanSource',
    label: 'Από',
    type: 'text',
    description: '',
    required: true
};

export class StageDescription {
    title: string;
    prev: string[];
    next: string[];
    canGoBack: boolean;
    stageFields: StageFieldDescription [];
}

/* stages descriptions */
export const stageIds = ['1', '2', '3', '4', '5a', '5b', '6', '7', '8', '9', '10', '11', '12', '13'];
export const stagesIfLowCost = ['1', '2', '3', '4', '5a', 'UploadInvoice', '5b', '8', '6', '12', '13'];

export const stagesDescriptionMap = {
    '1': {
        title: 'Υποβολή αιτήματος', prev: [], next: ['2'], canGoBack: false,
        stageFields: [commentDesc]
    },
    '2': {
        title: 'Έγκριση επιστημονικού υπευθύνου', prev: ['1'], next: ['3'], canGoBack: true,
        stageFields: [checkNecessityDesc, checkFeasibilityDesc, commentDesc]
    },
    '3': {
        title: 'Έλεγχος χειριστή έργου', prev: ['2'], next: ['4'], canGoBack: true,
        stageFields: [analiftheiYpoxrewsiDesc, fundsAvailableDesc, loanDesc, loanSourceDesc, commentDesc]
    },
    '4': {
        title: 'Βεβαίωση Π.Ο.Υ', prev: ['3'], next: ['5a'], canGoBack: true,
        stageFields: [analiftheiYpoxrewsiDesc, fundsAvailableDesc, commentDesc]
    },
    // '5': { title: 'Έγκριση Διευθυντή/Υπεύθυνου Μονάδας', prev: ['4'], next: ['5a'], canGoBack: true, stageFields: [] },
    '5a': {
        title: 'Έγκριση Διατάκτη', prev: ['4'], next: ['5b', '6'], canGoBack: true,
        stageFields: [commentDesc]
    },
    'UploadInvoice': {
        title: 'Υποβολή τιμολογίου', prev: [], next: [], canGoBack: true,
        stageFields: [commentDesc]
    },
    '5b': {
        title: 'Έγκριση Διοικητικού Συμβουλίου', prev: ['5a'], next: ['6'], canGoBack: true,
        stageFields: [commentDesc]
    },
    '6': {
        title: 'Ανάρτηση στην Διαύγεια', prev: ['5b', '5a'], next: ['7'], canGoBack: false,
        stageFields: [commentDesc]
    },
    '7': {
        title: 'Καταχώρηση συνοδευτικού υλικού', prev: ['6'], next: ['8'], canGoBack: true,
        stageFields: [commentDesc]
    },
    '8': {
        title: 'Έλεγχος από ομάδα ελέγχου', prev: ['7'], next: ['9'], canGoBack: true,
        stageFields: [checkRegularityDesc, checkLegalityDesc, commentDesc]
    },
    '9': {
        title: 'Έλεγχος από Π.Ο.Υ', prev: ['8'], next: ['10'], canGoBack: true,
        stageFields: [checkRegularityDesc, checkLegalityDesc, commentDesc]
    },
    '10': {
        title: 'Έλεγχος Διατάκτη', prev: ['9'], next: ['11'], canGoBack: true,
        stageFields: [commentDesc]
    },
    '11': {
        title: 'Ανάρτηση εξόφλησης στη Διαύγεια', prev: ['10'], canGoBack: true, next: ['12'],
        stageFields: [commentDesc]
    },
    '12': {
        title: 'Λογιστική καταχώρηση', prev: ['11'], next: ['13'], canGoBack: true,
        stageFields: [commentDesc]
    },
    '13': {
        title: 'Οικονομική διεκπεραίωση', prev: ['12'], next: [], canGoBack: true,
        stageFields: [commentDesc]
    }
};


/* old stage descriptions - not used anymore */
export const Stage2Desc = { id: '2', delegateField: 'scientificCoordinator',
    stageFields: [checkNecessityDesc, checkFeasibilityDesc] };
export const Stage3Desc = { id: '3', delegateField: 'operator',
    stageFields: [analiftheiYpoxrewsiDesc, fundsAvailableDesc, loanDesc, loanSourceDesc] };
export const Stage4Desc = { id: '4', delegateField: 'POI',
    stageFields: [analiftheiYpoxrewsiDesc, fundsAvailableDesc] };
export const Stage5Desc = { id: '5', delegateField: 'instituteDirector', stageFields: [] };
export const Stage5aDesc = { id: '5a', delegateField: 'organizationDirector', stageFields: [] };
export const Stage5bDesc = { id: '5b', delegateField: 'dioikitikoSumvoulio', stageFields: [] };
export const Stage6Desc = { id: '6', delegateField: 'organizationDiaugeia', stageFields: [] };
export const Stage7Desc = { id: '7', delegateField: 'operator', stageFields: [] };
export const Stage8Desc = { id: '8', delegateField: 'accountingDirector',
    stageFields: [checkRegularityDesc, checkLegalityDesc] };
export const Stage9Desc = { id: '9', delegateField: 'POI',
    stageFields: [checkRegularityDesc, checkLegalityDesc] };
export const Stage10Desc = { id: '10', delegateField: 'organizationDirector', stageFields: [] };
export const Stage11Desc = { id: '11', delegateField: 'organizationDiaugeia', stageFields: [] };
export const Stage12Desc = { id: '12', delegateField: 'accountingRegistration', stageFields: [] };
export const Stage13Desc = { id: '13', delegateField: 'accountingPayment', stageFields: [] };

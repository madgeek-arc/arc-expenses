/* shared constants */

/* NEXT TIME WE EMPTY THE DB REMEMBER TO:
    CHANGE 'regular' TO 'supply' AND USE supplierSelectionMethod terms in english
    CHANGE 'accepted' status TO 'completed'
    ADD english terms as values in supplierSelectionMethops (see supplierSelectionMethodsMap) */

export const requestTypes = {regular: 'Προμήθεια', trip: 'Ταξίδι', contract: 'Σύμβαση Έργου', services_contract: 'Σύμβαση Υπηρεσίας'};

export const statesList = ['all', 'accepted', 'pending', 'under_review', 'rejected'];
/*export const statesList = ['all', 'accepted', 'pending', 'rejected'];*/

export const supplierSelectionMethods = ['Απ\' ευθείας ανάθεση', 'Έρευνα αγοράς', 'Διαγωνισμός'];
export const supplierSelectionMethodsMap = {
    direct: 'Απ\' ευθείας ανάθεση', market_investigation: 'Έρευνα αγοράς', competition: 'Διαγωνισμός' };

/* field descriptions for the stages forms */
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
    stageFields: StageFieldDescription [];
}

/* stages descriptions */
export const stageIds = ['1', '2', '3', '4', '5a', '5b', '6', '7', '8', '9', '10', '11', '12', '13'];
export const approvalStages = ['1', '2', '3', '4', '5a', '5b', '6'];
export const paymentStages = ['7', '8', '9', '10', '11', '12', '13'];
/*export const stagesIfLowCost = ['1', '2', '3', '4', '5a', 'UploadInvoice', '5b', '8', '6', '12', '13'];*/

export const stageTitles = {
    '1': 'Υποβολή αιτήματος',
    '2': 'Έγκριση επιστημονικού υπευθύνου',
    '3': 'Έλεγχος χειριστή έργου',
    '4': 'Βεβαίωση Π.Ο.Υ.',
    '5a': 'Έγκριση Διατάκτη',
    // 'UploadInvoice': 'Υποβολή τιμολογίου',
    '5b': 'Έγκριση Διοικητικού Συμβουλίου',
    '6': 'Ανάρτηση στην Διαύγεια',
    '7': 'Καταχώρηση συνοδευτικού υλικού',
    '8': 'Έλεγχος από ομάδα ελέγχου',
    '9': 'Έλεγχος από Π.Ο.Υ',
    '10': 'Έλεγχος Διατάκτη',
    '11': 'Ανάρτηση εξόφλησης στη Διαύγεια',
    '12': 'Λογιστική καταχώρηση',
    '13': 'Οικονομική διεκπεραίωση'
};

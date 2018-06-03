/* the descriptions are used for dynamically loading request update forms and viewing results per stage */

export class StageFieldDescription {
  label: string;
  type: string;
  description: string;
  required: boolean;
}

export class StageDescription {
    id: string;
    stageFields: StageFieldDescription [];
    delegateField: string;
}

export const commentDesc = {
  label: 'Σχόλια',
  type: 'textarea',
  description: '',
  required: true
};

export const fundsAvailableDesc = {
  label: 'Υπάρχει διαθέσιμη πίστωση;',
  type: 'checkbox',
  description: '',
  required: true
};

export const analiftheiYpoxrewsiDesc = {
  label: 'Έχει αναληφθεί η υποχρέωση;',
  type: 'checkbox',
  description: '',
  required: true
};

export const checkRegularityDesc = {
  label: 'Έγινε έλεγχος κανονικότητας;',
  type: 'checkbox',
  description: '',
  required: true
};

export const checkLegalityDesc = {
  label: 'Έγινε έλεγχος νομιμότητας;',
  type: 'checkbox',
  description: '',
  required: true
};

export const checkNecessityDesc = {
  label: 'Έγινε έλεγχος αναγκαιότητας;',
  type: 'checkbox',
  description: '',
  required: true
};

export const checkFeasibilityDesc = {
  label: 'Έγινε έλεγχος σκοπιμότητας;',
  type: 'checkbox',
  description: '',
  required: true
};

export const accountCodeDesc = {
  label: 'Κωδικός λογιστικής (*)',
  type: 'text',
  description: '',
  required: true
};

export const accountDescriptionDesc = {
  label: 'Περιγραφή (*)',
  type: 'textarea',
  description: '',
  required: true
};

export  const loanDesc = {
    label: 'Έγινε δανεισμός',
    type: 'checkbox',
    description: '',
    required: true
};

export  const loanSourceDesc = {
    label: 'Από',
    type: 'text',
    description: '',
    required: true
};

export const Stage2Desc = {
    id: '2',
    delegateField: 'scientificCoordinator',
    stageFields: [checkNecessityDesc, checkFeasibilityDesc]
};

export const Stage3Desc = {
    id: '3',
    delegateField: 'operator',
    stageFields: [analiftheiYpoxrewsiDesc, fundsAvailableDesc, loanDesc, loanSourceDesc]
};

export const Stage4Desc = {
    id: '4',
    delegateField: 'POI',
    stageFields: [analiftheiYpoxrewsiDesc, fundsAvailableDesc]
};

export const Stage5Desc = {
    id: '5',
    delegateField: 'instituteDirector',
    stageFields: []
};

export const Stage5aDesc = {
    id: '5a',
    delegateField: 'organizationDirector',
    stageFields: []
};

export const Stage5bDesc = {
    id: '5b',
    delegateField: 'dioikitikoSumvoulio',
    stageFields: []
};

export const Stage6Desc = {
    id: '6',
    delegateField: 'organizationDiaugeia',
    stageFields: []
};

export const Stage7Desc = {
    id: '7',
    delegateField: 'operator',
    stageFields: []
};

export const Stage8Desc = {
    id: '8',
    delegateField: 'accountingDirector',
    stageFields: [checkRegularityDesc, checkLegalityDesc]
};

export const Stage9Desc = {
    id: '9',
    delegateField: 'POI',
    stageFields: [checkRegularityDesc, checkLegalityDesc]
};

export const Stage10Desc = {
    id: '10',
    delegateField: 'organizationDirector',
    stageFields: []
};

export const Stage11Desc = {
    id: '11',
    delegateField: 'organizationDiaugeia',
    stageFields: []
};

export const Stage12Desc = {
    id: '12',
    delegateField: 'accountingRegistration',
    stageFields: []
};

export const Stage13Desc = {
    id: '13',
    delegateField: 'accountingPayment',
    stageFields: []
};


/* will be used everytime a stage title is needed */
export const stagesMap = {
    'all': 'Όλα',
    '1': 'Υποβολή αιτήματος',
    '2': 'Έγκριση επιστημονικού υπευθύνου',
    '3': 'Έλεγχος χειριστή έργου',
    '4': 'Βεβαίωση Π.Ο.Υ',
    '5': 'Έγκριση Διευθυντή/Υπεύθυνου Μονάδας',
    '5a': 'Έγκριση Διατάκτη',
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

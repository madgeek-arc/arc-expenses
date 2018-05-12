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

export const isNecessaryDesc = {
  label: 'Έγινε έλεγχος αναγκαιότητας;',
  type: 'checkbox',
  description: '',
  required: true
};

export const isAdvisableDesc = {
  label: 'Έγινε έλεγχος σκοπιμότητας;',
  type: 'checkbox',
  description: '',
  required: true
};

export const accountingCodeDesc = {
  label: 'Κωδικός λογιστικής',
  type: 'text',
  description: '',
  required: true
};

export const accountingDescriptionDesc = {
  label: 'Περιγραφή',
  type: 'textarea',
  description: '',
  required: true
};

export const Stage2Desc = {
    id: '2',
    delegateField: 'scientificCoordinator',
    stageFields: [isNecessaryDesc, isAdvisableDesc]
};

export const Stage3Desc = {
    id: '3',
    delegateField: 'operator',
    stageFields: [analiftheiYpoxrewsiDesc, fundsAvailableDesc]
};

export const Stage4Desc = {
    id: '4',
    delegateField: 'POY',
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
    delegateField: 'POY',
    stageFields: [checkRegularityDesc, checkLegalityDesc]
};

export const Stage10Desc = {
    id: '10',
    delegateField: 'accountingRegistration',
    stageFields: [accountingCodeDesc, accountingDescriptionDesc]
};

export const Stage11Desc = {
    id: '11',
    delegateField: 'organizationDiaugeia',
    stageFields: []
};

export const Stage12Desc = {
    id: '12',
    delegateField: 'accountingPayment',
    stageFields: []
};


/* will be used everytime a stage title is needed */
export const stagesMap = {
    '2': 'Έγκριση επιστημονικού υπευθύνου',
    '3': 'Υποβολή αιτήματος',
    '4': 'Βεβαίωση Π.Ο.Υ',
    '5': 'Έγκριση Διευθυντή/Υπεύθυνου Μονάδας',
    '5a': 'Έγκριση Γενικού Διευθυντή',
    '5b': 'Έγκριση Διοικητικού Συμβουλίου',
    '6': 'Ανάρτηση στην Διαύγεια',
    '7': 'Καταχώρηση συνοδευτικού υλικού',
    '8': 'Έλεγχος από ομάδα ελέγχου',
    '9': 'Έλεγχος από Π.Ο.Υ',
    '10': 'Λογιστική καταχώρηση',
    '11': 'Ανάρτηση εξόφλησης στη Διαύγεια',
    '12': 'Οικονομική διεκπεραίωση'
};

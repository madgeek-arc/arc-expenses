export class StageFieldDescription {
  label: string;
  type: string;
  description: string;
  required: boolean;
}

export class StageDescription {
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
  delegateField: 'scientificCoordinator',
  stageFields: [isNecessaryDesc, isAdvisableDesc]
};

export const Stage3Desc = {
  delegateField: 'operator',
  stageFields: [analiftheiYpoxrewsiDesc, fundsAvailableDesc]
};

export const Stage4Desc = {
  delegateField: 'POY',
  stageFields: [analiftheiYpoxrewsiDesc, fundsAvailableDesc]
};

export const Stage5Desc = {
  delegateField: 'instituteDirector',
  stageFields: []
};

export const Stage5aDesc = {
    delegateField: 'organizationDirector',
    stageFields: []
};

export const Stage5bDesc = {
    delegateField: 'dioikitikoSumvoulio',
    stageFields: []
};

export const Stage6Desc = {
  delegateField: 'organizationDiaugeia',
  stageFields: []
};

export const Stage7Desc = {
    delegateField: 'operator',
    stageFields: []
};

export const Stage8Desc = {
  delegateField: 'accountingDirector',
  stageFields: [checkRegularityDesc, checkLegalityDesc]
};

export const Stage9Desc = {
  delegateField: 'POY',
  stageFields: [checkRegularityDesc, checkLegalityDesc]
};

export const Stage10Desc = {
  delegateField: 'accountingRegistration',
  stageFields: [accountingCodeDesc, accountingDescriptionDesc]
};

export const Stage11Desc = {
    delegateField: 'organizationDiaugeia',
    stageFields: []
};

export const Stage12Desc = {
  delegateField: 'accountingPayment',
  stageFields: []
};

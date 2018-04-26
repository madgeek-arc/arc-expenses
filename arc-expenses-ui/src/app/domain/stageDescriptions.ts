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
  label: 'Έγινε έλεγχος ορθότητας;',
  type: 'checkbox',
  description: '',
  required: true
};

export const checkLegalityDesc = {
  label: 'Έγινε νομικός έλεγχος;',
  type: 'checkbox',
  description: '',
  required: true
};

export const Stage2Desc = {
  delegateField: 'scientificCoordinator',
  stageFields: []
};

export const Stage3Desc = {
  delegateField: 'operator',
  stageFields: [analiftheiYpoxrewsiDesc, fundsAvailableDesc]
};

export const Stage3aDesc = {
  delegateField: 'organizationDirector',
  stageFields: []
};

export const Stage3bDesc = {
  delegateField: 'dioikitikoSumvoulio',
  stageFields: []
};

export const Stage4Desc = {
  delegateField: 'POY',
  stageFields: [analiftheiYpoxrewsiDesc, fundsAvailableDesc]
};

export const Stage5Desc = {
  delegateField: 'instituteDirector',
  stageFields: []
};

export const Stage6Desc = {
  delegateField: 'organizationDiaugeia',
  stageFields: []
};

export const Stage7Desc = {
  delegateField: 'accountingDirector',
  stageFields: [checkRegularityDesc, checkLegalityDesc]
};

export const Stage8Desc = {
  delegateField: 'POY',
  stageFields: [checkRegularityDesc, checkLegalityDesc]
};

export const Stage9Desc = {
  delegateField: 'accountingRegistration',
  stageFields: []
};

export const Stage10Desc = {
  delegateField: 'accountingPayment',
  stageFields: []
};

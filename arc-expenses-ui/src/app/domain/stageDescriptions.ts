import {variable} from '@angular/compiler/src/output/output_ast';

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

export let approvedDesc = {
  label: '',
  type: 'boolean',
  description: '',
  required: true
};

export const fundsAvailableDesc = {
  label: '',
  type: 'boolean',
  description: '',
  required: false
};

export const analiftheiYpoxrewsiDesc = {
  label: '',
  type: 'boolean',
  description: '',
  required: false
};

export const checkRegularityDesc = {
  label: '',
  type: 'boolean',
  description: '',
  required: ''
};

export const checkLegalityDesc = {
  label: '',
  type: 'boolean',
  description: '',
  required: ''
};

export const Stage2Desc = {
  delegateField: 'scientificCoordinator',
  stageFields: [approvedDesc]
};

export const Stage3Desc = {
  delegateField: 'operator',
  stageFields: [analiftheiYpoxrewsiDesc, fundsAvailableDesc, approvedDesc]
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
  stageFields: []
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
  stageFields: []
};

export const Stage8Desc = {
  delegateField: 'POY',
  stageFields: []
};

export const Stage9Desc = {
  delegateField: 'accountingRegistration',
  stageFields: []
};

export const Stage10Desc = {
  delegateField: 'accountingPayment',
  stageFields: []
};

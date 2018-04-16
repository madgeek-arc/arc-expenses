export class Request {

    id = 0;
    name = '';
    institute = '';
    program = '';
    position = '';
    requestText = '';
    supplier = '';
    supplierSelectionMethod = '';
    ammount = 0;

    constructor(
        public id: number,
        public name: string,
        public institute: string,
        public position: string,
        public program: string,
        public supplier: string,
        public supplierSelectionMethod: string,
        public ammount: number,
        public requestText?: string
    ) {  }
}

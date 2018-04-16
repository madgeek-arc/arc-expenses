export class Request {
    constructor(
        public id: number,
        public name: string,
        public institute: string,
        public position: string,
        public program: string,
        public supplier: string,
        public ssm: string,
        public ammount: number,
        public requestText?: string
    ) {  }


}

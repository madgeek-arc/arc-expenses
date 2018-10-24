/**
 * Created by stefania on 9/10/16.
 */
export class Facet {

    field: string;
    label: string;

    values: FacetValue[];
}

export interface FacetValue {

    value: string;
    label: string;
    count: number;
    isChecked: boolean;
}

export class SearchResults<T> {

    from: number;
    to: number;
    total: number;

    results: T[];
    facets: Facet[];
}

/* other extra classes */
export class Occurences {
    values: any;
}

export class Paging<T> {

    total: number;
    from: number;
    to: number;

    results: T[];
    occurences: Occurences;
}

// TODO: send a SearchParams at getAll instead of many query params
export class SearchParams {
    email: string;
    from: string;
    quantity: string;
    order: string;
    orderField: string;
    searchField: string;
    requestTypes: string[];
    statuses: string[];
    stages: string[];
    projects: string[];
    institutes: string[];

    constructor(email: string, from: string, quantity: string, order: string, orderField: string,
                searchField: string, statuses: string[], stages: string[], projects: string[], institutes: string[]) {
        this.email = email;
        this.from = from;
        this.quantity = quantity;
        this.order = order;
        this.orderField = orderField;
        this.searchField = searchField;
        this.statuses = statuses;
        this.stages = stages;
        this.projects = projects;
        this.institutes = institutes;
    }
}

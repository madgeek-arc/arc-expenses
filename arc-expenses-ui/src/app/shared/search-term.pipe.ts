import { Pipe, PipeTransform } from '@angular/core';
import { Vocabulary } from '../domain/operation';

@Pipe ({ name: 'filterByTerm' })
export class FilterByTerm implements PipeTransform {
    transform (items: Vocabulary[], searchTerm: string): any[] {
        if (!items) { return []; }
        if (!searchTerm) { return items; }

        searchTerm = searchTerm.trim();
        searchTerm = searchTerm.toLowerCase();

        return items.filter(item => item.projectAcronym.toLowerCase().includes(searchTerm));
    }
}


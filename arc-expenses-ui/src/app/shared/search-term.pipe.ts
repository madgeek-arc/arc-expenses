import { Pipe, PipeTransform } from '@angular/core';

@Pipe ({ name: 'filterByTerm' })
export class FilterByTerm implements PipeTransform {
    transform (items: string[], searchTerm: string): any[] {
        console.log(`items are: ${items}`);
        if (!items) { return []; }
        if (!searchTerm) { return items; }

        searchTerm = searchTerm.trim();
        searchTerm = searchTerm.toLowerCase();

        return items.filter(item => item.toLowerCase().includes(searchTerm));
    }
}


import { WebPlugin } from '@capacitor/core';

import type { LocationTrackerPlugin } from './definitions';

export class LocationTrackerWeb extends WebPlugin implements LocationTrackerPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}

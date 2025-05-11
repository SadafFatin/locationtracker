import { WebPlugin } from '@capacitor/core';

import type { CallbackError, LocationTrackerPlugin, WatchOptions } from './definitions';

export class LocationTrackerWeb extends WebPlugin implements LocationTrackerPlugin {
  
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }



  trackLocation(options: WatchOptions,callback: (position?: Location, error?: CallbackError) => void): Promise<any> {
    
    console.log(callback, options)
    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition((position) => {
        resolve({ value: position });
      });
    });
  }

  stopTrackingLocation(): Promise<{ value: any; }> {
    return new Promise((resolve) => {
      navigator.geolocation.clearWatch(0);
      resolve({ value: true });
    });
  }




}

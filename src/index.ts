import { registerPlugin } from '@capacitor/core';

import type { LocationTrackerPlugin } from './definitions';

const LocationTracker = registerPlugin<LocationTrackerPlugin>('LocationTracker', {
  web: () => import('./web').then((m) => new m.LocationTrackerWeb()),
});

export * from './definitions';
export { LocationTracker };

import { registerPlugin } from '@capacitor/core';
import type { LocationTrackerPlugin } from './definitions';

const LocationTracker = registerPlugin<LocationTrackerPlugin>('LocationTracker');

export * from './definitions';
export { LocationTracker };

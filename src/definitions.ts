
export interface CallbackError extends Error {
  code?: string;
}

export interface WatchOptions {
  baseUrl: string;
  data: any
}

export interface LocationTrackerPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  trackLocation(

    options: WatchOptions,
    callback: (
    position?: Location,
    error?: CallbackError
  ) => void
  ): Promise<string>;

  stopTrackingLocation(): Promise<{ value: any }>;

}



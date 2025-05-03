export interface LocationTrackerPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}

# location--tracker

Tracking User Location by Background Service

## Install

```bash
npm install location--tracker
npx cap sync
```

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`trackLocation(...)`](#tracklocation)
* [`stopTrackingLocation()`](#stoptrackinglocation)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### trackLocation(...)

```typescript
trackLocation(options: WatchOptions, callback: (position?: any, error?: CallbackError | undefined) => void) => Promise<string>
```

| Param          | Type                                                                                         |
| -------------- | -------------------------------------------------------------------------------------------- |
| **`options`**  | <code><a href="#watchoptions">WatchOptions</a></code>                                        |
| **`callback`** | <code>(position?: any, error?: <a href="#callbackerror">CallbackError</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;string&gt;</code>

--------------------


### stopTrackingLocation()

```typescript
stopTrackingLocation() => Promise<{ value: any; }>
```

**Returns:** <code>Promise&lt;{ value: any; }&gt;</code>

--------------------


### Interfaces


#### WatchOptions

| Prop          | Type                |
| ------------- | ------------------- |
| **`baseUrl`** | <code>string</code> |


#### CallbackError

| Prop       | Type                |
| ---------- | ------------------- |
| **`code`** | <code>string</code> |

</docgen-api>

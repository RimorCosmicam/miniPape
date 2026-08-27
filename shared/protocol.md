# miniPape local protocol — v1

The Android app is the server. The Mac discovers or manually connects to the phone, then pairs using the six-digit code shown by the phone.

Default TCP port: `47977`.

## Endpoints

### `GET /v1/status`

Returns JSON containing protocol version, device model, canvas dimensions, receiver name, and whether the supplied `X-miniPape-Code` is paired.

### `POST /v1/preview/source`

Uploads the source once for a transient preview session.

### `PUT /v1/preview/state`

Updates crop, scale, rotation, playback position, mute, loop state, and an ordered `filters` array as compact JSON. Filter identifiers use miniMate's uppercase enum names; duplicates and order are preserved. The phone applies the recipe locally so live editing does not repeatedly resend video frames.

### `POST /v1/wallpapers`

Stores a completed asset. Headers carry UTF-8 name, media type, and a Base64-encoded non-destructive recipe containing crop, playback, and filter-stack state. The body is the encoded image, GIF, or video.

## Security

- Receiver binds to the local interface only.
- Pair codes rotate whenever receiver mode is restarted.
- Every mutating endpoint requires the current pair code.
- Maximum upload size defaults to 250 MB.
- Filenames are normalized by the receiver and never used as filesystem paths directly.

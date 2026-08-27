import Foundation
import Observation

enum PhoneClientError: LocalizedError {
    case invalidAddress
    case rejected(Int)

    var errorDescription: String? {
        switch self {
        case .invalidAddress: "Enter the address shown by miniPape on your phone."
        case let .rejected(code): "The phone rejected the request (HTTP \(code))."
        }
    }
}

@MainActor
@Observable
final class PhoneClient {
    var address = ""
    var pairCode = ""
    private(set) var status: PhoneStatus?
    private(set) var isSending = false
    private(set) var message = "Not connected"

    private let session = URLSession(configuration: .ephemeral)

    func connect() async throws {
        let request = try request(path: "/v1/status", method: "GET")
        let (data, response) = try await session.data(for: request)
        try validate(response)
        status = try JSONDecoder().decode(PhoneStatus.self, from: data)
        message = "Connected to \(status?.receiverName ?? "phone")"
    }

    func startPreview(fileURL: URL, kind: MediaKind) async throws {
        isSending = true
        defer { isSending = false }
        var request = try request(path: "/v1/preview/source", method: "POST")
        request.setValue(kind.rawValue, forHTTPHeaderField: "X-miniPape-Media-Kind")
        request.setValue(fileURL.lastPathComponent, forHTTPHeaderField: "X-miniPape-Name")
        let (_, response) = try await session.upload(for: request, fromFile: fileURL)
        try validate(response)
        message = "Live preview ready"
    }

    func updatePreview(_ state: PreviewState) async throws {
        var request = try request(path: "/v1/preview/state", method: "PUT")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        let data = try JSONEncoder().encode(state)
        let (_, response) = try await session.upload(for: request, from: data)
        try validate(response)
    }

    func sendWallpaper(fileURL: URL, kind: MediaKind, recipe: CropRecipe) async throws {
        isSending = true
        defer { isSending = false }
        var request = try request(path: "/v1/wallpapers", method: "POST")
        request.setValue(kind.rawValue, forHTTPHeaderField: "X-miniPape-Media-Kind")
        request.setValue(fileURL.lastPathComponent, forHTTPHeaderField: "X-miniPape-Name")
        request.setValue(try JSONEncoder().encode(recipe).base64EncodedString(), forHTTPHeaderField: "X-miniPape-Crop")
        let (_, response) = try await session.upload(for: request, fromFile: fileURL)
        try validate(response)
        message = "Sent to phone"
    }

    private func request(path: String, method: String) throws -> URLRequest {
        let value = address.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalized = value.hasPrefix("http://") || value.hasPrefix("https://") ? value : "http://\(value)"
        guard var components = URLComponents(string: normalized), components.host != nil else {
            throw PhoneClientError.invalidAddress
        }
        if components.port == nil { components.port = 47977 }
        components.path = path
        guard let url = components.url else { throw PhoneClientError.invalidAddress }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.timeoutInterval = 15
        request.setValue(pairCode, forHTTPHeaderField: "X-miniPape-Code")
        return request
    }

    private func validate(_ response: URLResponse) throws {
        guard let http = response as? HTTPURLResponse, 200..<300 ~= http.statusCode else {
            throw PhoneClientError.rejected((response as? HTTPURLResponse)?.statusCode ?? 0)
        }
    }
}


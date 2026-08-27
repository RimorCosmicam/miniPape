import XCTest
@testable import miniPapeMac

final class ModelTests: XCTestCase {
    func testFlipCanvasMatchesDeviceSpecification() {
        XCTAssertEqual(FlipCanvas.zFlip7.width, 1048)
        XCTAssertEqual(FlipCanvas.zFlip7.height, 948)
    }

    func testCropRecipeRoundTrips() throws {
        var recipe = CropRecipe()
        recipe.scale = 1.75
        recipe.offsetX = -0.2
        let decoded = try JSONDecoder().decode(CropRecipe.self, from: JSONEncoder().encode(recipe))
        XCTAssertEqual(decoded, recipe)
    }
}


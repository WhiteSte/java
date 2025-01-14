/**
 * **************************************************************************** Copyright (c) 2016, hobu Inc.
 * (info@hobu.co)
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer. * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution. * Neither the
 * name of Hobu, Inc. nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.pdal

import io.circe.parser
import java.nio.{ByteBuffer, ByteOrder}

import scala.collection.JavaConverters._

class PipelineSpec extends TestEnvironmentSpec {
  describe("Pipeline execution") {
    it("should validate as incorrect json (bad json passed)") {
      val badPipeline = Pipeline(badJson)
      badPipeline.validate() should be(false)
      badPipeline.close()
      badPipeline.ptr() should be(0)
    }

    it("should validate json") {
      pipeline.validate() should be(true)
    }

    it("should execute pipeline") {
      pipeline.execute()
    }

    it("should create pointViews iterator") {
      val pvi = pipeline.getPointViews()
      pvi.asScala.length should be(1)
      pvi.close()
    }

    it("should have a valid point view size") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()
      pv.length() should be(1065)
      pvi.hasNext should be(false)
      pv.close()
      pvi.close()
    }

    it("should read a valid (X, Y, Z) data") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()
      pv.getX(0) should be(637012.24)
      pv.getY(0) should be(849028.31)
      pv.getZ(0) should be(431.66)
      pv.close()
      pvi.close()
    }

    it("should read a valid packed data") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()
      val layout = pv.layout()
      val arr = pv.getPackedPoint(0, Array(DimType.X, DimType.Y))
      val (xarr, yarr) = arr.take(layout.dimSize(DimType.X).toInt) -> arr.drop(layout.dimSize(DimType.Y).toInt)

      ByteBuffer.wrap(xarr).order(ByteOrder.nativeOrder()).getDouble should be(pv.getX(0))
      ByteBuffer.wrap(yarr).order(ByteOrder.nativeOrder()).getDouble should be(pv.getY(0))

      layout.close()
      pv.close()
      pvi.close()
    }

    it("should read the whole packed point and grab only one dim") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()
      val arr = pv.getPackedPoint(0)
      pv.get(arr, DimType.Y).getDouble should be(pv.getY(0))
      pv.close()
      pvi.close()
    }

    it("should read all packed points and grab only one point out of it") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()
      pv.get(3, pv.getPackedPoints()) should be(pv.getPackedPoint(3))
      pv.close()
      pvi.close()
    }

    it("should read a valid value by name") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()
      pv.getByte(0, "ReturnNumber") should be(1)
      pv.close()
      pvi.close()
    }

    it("should read correctly data as a packed point") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()
      val layout = pv.layout()
      val arr = pv.getPackedPoint(0)
      layout.dimTypes().foreach { dt => pv.get(0, dt).array() should be(pv.get(arr, dt).array()) }
      layout.close()
      pv.close()
      pvi.close()
    }

    it("layout should have a valid number of dims") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()
      pv.layout().dimTypes().length should be(20)
      pv.close()
      pvi.close()
    }

    it("should find a dim by name") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()
      pv.findDimType("Red") should be(DimType("Red", "uint16_t"))
      pv.close()
      pvi.close()
    }

    it("dim sizes should be of a valid size") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()
      val layout = pv.layout()
      layout.dimTypes().map(pv.layout().dimSize(_)).sum should be(layout.pointSize())
      layout.close()
      pv.close()
      pvi.close()
    }

    it("should read all packed points valid") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()
      val layout = pv.layout()
      pv.getPackedPoints().length should be(pv.length() * layout.pointSize())
      layout.close()
      pv.close()
      pvi.close()
    }

    it("should read crs correct") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()
      pv.getCrsProj4() should be(proj4String).or(be(proj4StringMac))
      pv.close()
      pvi.close()
    }

    it("should fail with InitializationException when the input json is null") {
      intercept[InitializationException] { Pipeline(null) }
    }

    it("should fail with ExecutionException when the input json is invalid") {
      intercept[InitializationException] { Pipeline("{") }
    }

    it("should get pipeline") {
      parser.parse(pipeline.getPipeline()) shouldBe jsonExpectedJson
    }

    it("should get schema") {
      parser.parse(pipeline.getSchema()) shouldBe schemaJson
    }

    it("should get metadata") {
      parser.parse(pipeline.getMetadata()) shouldBe metadataJson
    }

    it("should get quickInfo") {
      val p = Pipeline(json)
      parser.parse(p.getQuickInfo()) shouldBe quickInfoJson
    }

    it("should get quickInfo with metadata") {
      val p = Pipeline(json)
      p.execute()
      parser.parse(p.getQuickInfo()) should be(quickInfoWithMetadataJson).or(be(quickInfoWithMetadataMacJson))
    }

    it("should extract mesh in iterative fashion") {
      pipelineDelaunay.validate() should be(true)
      pipelineDelaunay.execute()
      val pvi = pipelineDelaunay.getPointViews()
      val pv = pvi.next()
      val mesh = pv.getTriangularMesh()
      val actual = mesh.asScala.toList

      actual should be(expectedDelaunayPlyTriangles)

      mesh.close()
      pv.close()
      pvi.close()
    }

    it("should extract mesh as a bulk") {
      val pvi = pipelineDelaunay.getPointViews()
      val pv = pvi.next()
      val mesh = pv.getTriangularMesh()

      val actual = mesh.asArray().toList

      actual should be(expectedDelaunayPlyTriangles)

      mesh.close()
      pv.close()
      pvi.close()
    }

    it("should extract mesh directly by a triangle identifier") {
      val pvi = pipelineDelaunay.getPointViews()
      val pv = pvi.next()
      val mesh = pv.getTriangularMesh()

      var i = 0
      while (i < mesh.size()) {
        mesh.get(i) shouldBe expectedDelaunayPlyTriangles(i)
        i += 1
      }

      mesh.close()
      pv.close()
      pvi.close()
    }

    it("should not extract mesh if it was not generated") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()

      intercept[ExecutionException] { pv.getTriangularMesh() }

      pv.close()
      pvi.close()
    }

    it("should rasterize mesh") {
      val pvi = pipelineDelaunay.getPointViews()
      val pv = pvi.next()

      val raster = pv.rasterizeTriangularMesh(Array(635619.85, 848899.7, 638982.55, 853535.43), 100, 100)

      val (mi, ma) = {
        val sorted = raster.filter(!_.isNaN).sorted
        sorted.head -> sorted.last
      }

      mi shouldBe 406.915 +- 1e-3
      ma shouldBe 582.022 +- 1e-3

      pv.close()
      pvi.close()
    }

    it("should not rasterize mesh if it was not generated") {
      val pvi = pipeline.getPointViews()
      val pv = pvi.next()

      intercept[ExecutionException] { pv.rasterizeTriangularMesh(Array(), 0, 0) }

      pv.close()
      pvi.close()
    }
  }
}

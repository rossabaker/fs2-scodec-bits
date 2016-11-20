/* The arbitrary ByteVectors are taken from scodec.bits.Arbitraries */
package fs2
package interop.scodec.bits

import java.nio.ByteBuffer

import scala.{Stream => SStream}
import scodec.bits.ByteVector

import org.scalacheck.{Gen, Arbitrary, Shrink}
import org.scalacheck.Arbitrary.arbitrary

import org.scalatest.{FreeSpec, Matchers}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class ByteVectorChunkSpec
    extends FreeSpec
    with GeneratorDrivenPropertyChecks
    with Matchers
{
  def standardByteVectors(maxSize: Int): Gen[ByteVector] = for {
    size <- Gen.choose(0, maxSize)
    bytes <- Gen.listOfN(size, Gen.choose(0, 255))
  } yield ByteVector(bytes: _*)

  val sliceByteVectors: Gen[ByteVector] = for {
    bytes <- arbitrary[Array[Byte]]
    toDrop <- Gen.choose(0, bytes.size)
  } yield ByteVector.view(bytes).drop(toDrop.toLong)

  def genSplitBytes(g: Gen[ByteVector]) = for {
    b <- g
    n <- Gen.choose(0, b.size+1)
  } yield {
    b.take(n) ++ b.drop(n)
  }

  def genConcatBytes(g: Gen[ByteVector]) =
    g.map { b => b.toIndexedSeq.foldLeft(ByteVector.empty)(_ :+ _) }

  def genByteBufferVectors(maxSize: Int): Gen[ByteVector] = for {
    size <- Gen.choose(0, maxSize)
    bytes <- Gen.listOfN(size, Gen.choose(0, 255))
  } yield ByteVector.view(ByteBuffer.wrap(bytes.map(_.toByte).toArray))

  val byteVectors: Gen[ByteVector] = Gen.oneOf(
    standardByteVectors(100),
    genConcatBytes(standardByteVectors(100)),
    sliceByteVectors,
    genSplitBytes(sliceByteVectors),
    genSplitBytes(genConcatBytes(standardByteVectors(500))),
    genByteBufferVectors(100))

  implicit val arbitraryByteVectors: Arbitrary[ByteVector] =
    Arbitrary(byteVectors)

  val byteVectorChunks: Gen[ByteVectorChunk] =
    byteVectors.map(ByteVectorChunk(_))

  implicit val aribtraryByteVectorChunks: Arbitrary[ByteVectorChunk] =
    Arbitrary(byteVectorChunks)

  implicit val shrinkByteVector: Shrink[ByteVector] =
    Shrink[ByteVector] { b =>
      if (b.nonEmpty)
        SStream.iterate(b.take(b.size / 2))(b2 => b2.take(b2.size / 2)).takeWhile(_.nonEmpty) ++ SStream(ByteVector.empty)
      else SStream.empty
    }

  "ByteVectorChunk" - {
    "size" in forAll { c: ByteVectorChunk =>
      c.size should be (c.toVector.size)
    }

    "take" in forAll { (c: ByteVectorChunk, n: Int) =>
      c.take(n).toVector should be (c.toVector.take(n))
    }

    "drop" in forAll { (c: ByteVectorChunk, n: Int) =>
      c.drop(n).toVector should be (c.toVector.drop(n))
    }

    "uncons" in forAll { c: ByteVectorChunk =>
      if (c.toVector.isEmpty)
        c.uncons.isEmpty
      else
        c.uncons.contains((c(0), c.drop(1)))
    }

    "isEmpty" in forAll { c: ByteVectorChunk =>
      c.isEmpty should be (c.toVector.isEmpty)
    }

    "filter" in forAll { (c: ByteVectorChunk, f: Byte => Boolean) =>
      c.filter(f).toVector should be (c.toVector.filter(f))
    }

    "foldLeft" in forAll { (c: ByteVectorChunk, z: Long, f: (Long, Byte) => Long) =>
      c.foldLeft(z)(f) should be (c.toVector.foldLeft(z)(f))
    }

    "foldRight" in forAll { (c: ByteVectorChunk, z: Long, f: (Byte, Long) => Long) =>
      c.foldRight(z)(f) should be (c.toVector.foldRight(z)(f))
    }

    "toArray" in forAll { c: ByteVectorChunk =>
      c.toArray.toVector should be (c.toVector)
    }

    "concat" in forAll { cs: List[ByteVectorChunk] =>
      val result = Chunk.concat(cs)
      result.toVector shouldBe cs.foldLeft(Vector.empty[Byte])(_ ++ _.toVector)
      if (!result.isEmpty) result shouldBe a[ByteVectorChunk]
      result
    }
  }
}

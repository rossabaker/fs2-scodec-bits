package fs2
package interop.scodec.bits

import scala.reflect.{ClassTag, classTag}
import scodec.bits.ByteVector

final class ByteVectorChunk private (bv: ByteVector)
    extends MonomorphicChunk[Byte]
{
  def apply(i: Int): Byte =
    bv(i)

  def copyToArray[B >: Byte](xs: Array[B], start: Int): Unit =
    xs match {
      case byteArray: Array[Byte] =>
        bv.copyToArray(byteArray, start)
      case _ =>
        bv.toArray.iterator.copyToArray(xs, start)
    }

  def drop(n: Int): Chunk[Byte] =
    ByteVectorChunk(bv.drop(n))

  def filter(f: Byte => Boolean): Chunk[Byte] = {
    var i = 0
    val bound = bv.size

    val values2 = new Array[Byte](size)
    var size2 = 0

    while (i < bound) {
      val b = bv(i)
      if (f(b)) {
        values2(size2) = bv(i)
        size2 += 1
      }

      i += 1
    }

    ByteVectorChunk(ByteVector.view(values2, 0, size2))
  }

  def foldLeft[B](z: B)(f: (B, Byte) => B): B =
    bv.foldLeft(z)(f)

  def foldRight[B](z: B)(f: (Byte, B) => B): B =
    bv.foldRight(z)(f)

  def size: Int =
    bv.size.toInt // bad truncation

  def take(n: Int): Chunk[Byte] =
    ByteVectorChunk(bv.take(n))

  protected val tag: ClassTag[_] =
    classTag[ByteVector]

}

object ByteVectorChunk {
  def apply(bv: ByteVector): ByteVectorChunk =
    new ByteVectorChunk(bv)
}

package com.fortysevendeg.repository.cachecategory

import com.fortysevendeg.ninecardslauncher.commons.exceptions.Exceptions.NineCardsException
import com.fortysevendeg.ninecardslauncher.repository.commons.{CacheCategoryUri, ContentResolverWrapperImpl}
import com.fortysevendeg.ninecardslauncher.repository.model.CacheCategory
import com.fortysevendeg.ninecardslauncher.repository.provider.CacheCategoryEntity._
import com.fortysevendeg.ninecardslauncher.repository.provider._
import com.fortysevendeg.ninecardslauncher.repository.repositories._
import com.fortysevendeg.repository._
import org.specs2.matcher.DisjunctionMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

trait CacheCategoryRepositorySpecification
  extends Specification
  with DisjunctionMatchers
  with Mockito {

  trait CacheCategoryRepositoryScope
    extends Scope {

    lazy val contentResolverWrapper = mock[ContentResolverWrapperImpl]
    lazy val cacheCategoryRepository = new CacheCategoryRepository(contentResolverWrapper)
  }

  trait ValidCacheCategoryRepositoryResponses
    extends DBUtils
    with CacheCategoryRepositoryTestData {

    self: CacheCategoryRepositoryScope =>

    contentResolverWrapper.insert(nineCardsUri = CacheCategoryUri, values = createCacheCategoryValues) returns testCacheCategoryId

    contentResolverWrapper.deleteById(nineCardsUri = CacheCategoryUri, id = testCacheCategoryId) returns 1

    contentResolverWrapper.delete(
      nineCardsUri = CacheCategoryUri,
      where = s"$packageName = ?",
      whereParams = Seq(testPackageName)) returns 1

    contentResolverWrapper.findById(
      nineCardsUri = CacheCategoryUri,
      id = testCacheCategoryId,
      projection = allFields)(
        f = getEntityFromCursor(cacheCategoryEntityFromCursor)) returns Some(cacheCategoryEntity)

    contentResolverWrapper.findById(
      nineCardsUri = CacheCategoryUri,
      id = testNonExistingCacheCategoryId,
      projection = allFields)(
        f = getEntityFromCursor(cacheCategoryEntityFromCursor)) returns None

    contentResolverWrapper.fetchAll(
      nineCardsUri = CacheCategoryUri,
      projection = allFields)(
        f = getListFromCursor(cacheCategoryEntityFromCursor)) returns cacheCategoryEntitySeq

    contentResolverWrapper.fetch(
      nineCardsUri = CacheCategoryUri,
      projection = allFields,
      where = s"$packageName = ?",
      whereParams = Seq(testPackageName))(
        f = getEntityFromCursor(cacheCategoryEntityFromCursor)) returns Some(cacheCategoryEntity)

    contentResolverWrapper.fetch(
      nineCardsUri = CacheCategoryUri,
      projection = allFields,
      where = s"$packageName = ?",
      whereParams = Seq(testNonExistingPackageName))(
        f = getEntityFromCursor(cacheCategoryEntityFromCursor)) returns None

    contentResolverWrapper.updateById(CacheCategoryUri, testCacheCategoryId, createCacheCategoryValues) returns 1
  }

  trait ErrorCacheCategoryRepositoryResponses
    extends DBUtils
    with CacheCategoryRepositoryTestData {

    self: CacheCategoryRepositoryScope =>

    val contentResolverException = new RuntimeException("Irrelevant message")

    contentResolverWrapper.insert(nineCardsUri = CacheCategoryUri, values = createCacheCategoryValues) throws contentResolverException

    contentResolverWrapper.deleteById(nineCardsUri = CacheCategoryUri, id = testCacheCategoryId) throws contentResolverException

    contentResolverWrapper.delete(
      nineCardsUri = CacheCategoryUri,
      where = s"$packageName = ?",
      whereParams = Seq(testPackageName)) throws contentResolverException

    contentResolverWrapper.findById(
      nineCardsUri = CacheCategoryUri,
      id = testCacheCategoryId,
      projection = allFields)(
        f = getEntityFromCursor(cacheCategoryEntityFromCursor)) throws contentResolverException

    contentResolverWrapper.fetchAll(
      nineCardsUri = CacheCategoryUri,
      projection = allFields)(
        f = getListFromCursor(cacheCategoryEntityFromCursor)) throws contentResolverException

    contentResolverWrapper.fetch(
      nineCardsUri = CacheCategoryUri,
      projection = allFields,
      where = s"$packageName = ?",
      whereParams = Seq(testPackageName))(
        f = getEntityFromCursor(cacheCategoryEntityFromCursor)) throws contentResolverException

    contentResolverWrapper.updateById(CacheCategoryUri, testCacheCategoryId, createCacheCategoryValues) throws contentResolverException
  }

}

trait CacheCategoryMockCursor
  extends MockCursor
  with DBUtils
  with CacheCategoryRepositoryTestData {

  val cursorData = Seq(
    (NineCardsSqlHelper.id, 0, cacheCategorySeq map (_.id), IntDataType),
    (packageName, 1, cacheCategorySeq map (_.data.packageName), StringDataType),
    (category, 2, cacheCategorySeq map (_.data.category), StringDataType),
    (starRating, 3, cacheCategorySeq map (_.data.starRating), DoubleDataType),
    (numDownloads, 4, cacheCategorySeq map (_.data.numDownloads), StringDataType),
    (ratingsCount, 5, cacheCategorySeq map (_.data.ratingsCount), IntDataType),
    (commentCount, 6, cacheCategorySeq map (_.data.commentCount), IntDataType))

  prepareCursor[CacheCategory](cacheCategorySeq.size, cursorData)
}

trait EmptyCacheCategoryMockCursor
  extends MockCursor
  with DBUtils
  with CacheCategoryRepositoryTestData {

  val cursorData = Seq(
    (NineCardsSqlHelper.id, 0, Seq.empty, IntDataType),
    (packageName, 1, Seq.empty, StringDataType),
    (category, 2, Seq.empty, StringDataType),
    (starRating, 3, Seq.empty, DoubleDataType),
    (numDownloads, 4, Seq.empty, StringDataType),
    (ratingsCount, 5, Seq.empty, IntDataType),
    (commentCount, 6, Seq.empty, IntDataType))

  prepareCursor[CacheCategory](0, cursorData)
}

class CacheCategoryRepositorySpec
  extends CacheCategoryRepositorySpecification {

  "CacheCategoryRepositoryClient component" should {

    "addCacheCategory" should {

      "return a CacheCategory object with a valid request" in
        new CacheCategoryRepositoryScope
          with ValidCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.addCacheCategory(data = createCacheCategoryData).run

          result must be_\/-[CacheCategory].which {
            cacheCategory =>
              cacheCategory.id shouldEqual testCacheCategoryId
              cacheCategory.data.packageName shouldEqual testPackageName
          }
        }

      "return a NineCardsException when a exception is thrown" in
        new CacheCategoryRepositoryScope
          with ErrorCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.addCacheCategory(data = createCacheCategoryData).run

          result must be_-\/[NineCardsException]
        }
    }

    "deleteCacheCategory" should {

      "return a successful response when a valid cache category id is given" in
        new CacheCategoryRepositoryScope
          with ValidCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.deleteCacheCategory(cacheCategory = cacheCategory).run

          result must be_\/-[Int].which(_ shouldEqual 1)
        }

      "return a NineCardsException when a exception is thrown" in
        new CacheCategoryRepositoryScope
          with ErrorCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.deleteCacheCategory(cacheCategory = cacheCategory).run

          result must be_-\/[NineCardsException]
        }
    }

    "deleteCacheCategoryByPackage" should {

      "return a successful response when a valid package name is given" in
        new CacheCategoryRepositoryScope
          with ValidCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.deleteCacheCategoryByPackage(packageName = testPackageName).run

          result must be_\/-[Int].which(_ shouldEqual 1)
        }

      "return a NineCardsException when a exception is thrown" in
        new CacheCategoryRepositoryScope
          with ErrorCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.deleteCacheCategoryByPackage(packageName = testPackageName).run

          result must be_-\/[NineCardsException]
        }
    }

    "fetchCacheCategories" should {

      "return all the cache categories stored in the database" in
        new CacheCategoryRepositoryScope
          with ValidCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.fetchCacheCategories.run

          result must be_\/-[Seq[CacheCategory]].which(_ shouldEqual cacheCategorySeq)
        }

      "return a NineCardsException when a exception is thrown" in
        new CacheCategoryRepositoryScope
          with ErrorCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.fetchCacheCategories.run

          result must be_-\/[NineCardsException]
        }
    }

    "findCacheCategoryById" should {

      "return a CacheCategory object when a existent id is given" in
        new CacheCategoryRepositoryScope
          with ValidCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.findCacheCategoryById(id = testCacheCategoryId).run

          result must be_\/-[Option[CacheCategory]].which {
            maybeCacheCategory =>
              maybeCacheCategory must beSome[CacheCategory].which { cacheCategory =>
                cacheCategory.id shouldEqual testCacheCategoryId
                cacheCategory.data.packageName shouldEqual testPackageName
              }
          }
        }

      "return None when a non-existent id is given" in
        new CacheCategoryRepositoryScope
          with ValidCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.findCacheCategoryById(id = testNonExistingCacheCategoryId).run

          result must be_\/-[Option[CacheCategory]].which(_ must beNone)
        }

      "return a NineCardsException when a exception is thrown" in
        new CacheCategoryRepositoryScope
          with ErrorCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.findCacheCategoryById(id = testCacheCategoryId).run

          result must be_-\/[NineCardsException]
        }
    }

    "fetchCacheCategoryByPackage" should {
      "return a CacheCategory object when a existent package name is given" in
        new CacheCategoryRepositoryScope
          with ValidCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.fetchCacheCategoryByPackage(packageName = testPackageName).run

          result must be_\/-[Option[CacheCategory]].which {
            maybeCacheCategory =>
              maybeCacheCategory must beSome[CacheCategory].which { cacheCategory =>
                cacheCategory.id shouldEqual testCacheCategoryId
                cacheCategory.data.packageName shouldEqual testPackageName
              }
          }
        }

      "return None when a non-existent package name is given" in
        new CacheCategoryRepositoryScope
          with ValidCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.fetchCacheCategoryByPackage(packageName = testNonExistingPackageName).run

          result must be_\/-[Option[CacheCategory]].which(_ must beNone)
        }

      "return a NineCardsException when a exception is thrown" in
        new CacheCategoryRepositoryScope
          with ErrorCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.fetchCacheCategoryByPackage(packageName = testPackageName).run

          result must be_-\/[NineCardsException]
        }
    }

    "updateCacheCategory" should {

      "return a successful response when the cache category is updated" in
        new CacheCategoryRepositoryScope
          with ValidCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.updateCacheCategory(cacheCategory = cacheCategory).run

          result must be_\/-[Int].which(_ shouldEqual 1)
        }

      "return a NineCardsException when a exception is thrown" in
        new CacheCategoryRepositoryScope
          with ErrorCacheCategoryRepositoryResponses {

          val result = cacheCategoryRepository.updateCacheCategory(cacheCategory = cacheCategory).run

          result must be_-\/[NineCardsException]
        }
    }

    "getEntityFromCursor" should {

      "return None when an empty cursor is given" in
        new EmptyCacheCategoryMockCursor
          with CacheCategoryRepositoryScope {

          val result = getEntityFromCursor(cacheCategoryEntityFromCursor)(mockCursor)

          result must beNone
        }

      "return a CacheCategory object when a cursor with data is given" in
        new CacheCategoryMockCursor
          with CacheCategoryRepositoryScope {

          val result = getEntityFromCursor(cacheCategoryEntityFromCursor)(mockCursor)

          result must beSome[CacheCategoryEntity].which { cacheCategory =>
            cacheCategory.id shouldEqual cacheCategoryEntity.id
            cacheCategory.data shouldEqual cacheCategoryEntity.data
          }
        }
    }

    "getListFromCursor" should {

      "return an empty sequence when an empty cursor is given" in
        new EmptyCacheCategoryMockCursor
          with CacheCategoryRepositoryScope {

          val result = getListFromCursor(cacheCategoryEntityFromCursor)(mockCursor)

          result shouldEqual Seq.empty
        }

      "return a CacheCategory sequence when a cursor with data is given" in
        new CacheCategoryMockCursor
          with CacheCategoryRepositoryScope {
          val result = getListFromCursor(cacheCategoryEntityFromCursor)(mockCursor)

          result shouldEqual cacheCategoryEntitySeq
        }
    }
  }

}

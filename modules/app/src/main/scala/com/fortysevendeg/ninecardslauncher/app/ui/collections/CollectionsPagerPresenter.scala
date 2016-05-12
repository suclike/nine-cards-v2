package com.fortysevendeg.ninecardslauncher.app.ui.collections

import android.content.Intent
import android.graphics.Bitmap
import com.fortysevendeg.ninecardslauncher.app.commons.NineCardIntentConversions
import com.fortysevendeg.ninecardslauncher.app.ui.commons.Presenter
import com.fortysevendeg.ninecardslauncher.app.ui.commons.TasksOps._
import com.fortysevendeg.ninecardslauncher.commons.services.Service
import com.fortysevendeg.ninecardslauncher.commons.services.Service._
import com.fortysevendeg.ninecardslauncher.process.collection.{AddCardRequest, CardException}
import com.fortysevendeg.ninecardslauncher.process.commons.models.{Card, Collection}
import com.fortysevendeg.ninecardslauncher.process.commons.types.ShortcutCardType
import com.fortysevendeg.ninecardslauncher.process.device.ShortcutException
import macroid.{ActivityContextWrapper, Ui}
import rapture.core.Result

import scala.util.Random
import scalaz.concurrent.Task

class CollectionsPagerPresenter(
  actions: CollectionsUiActions)(implicit activityContextWrapper: ActivityContextWrapper)
  extends Presenter
  with NineCardIntentConversions { self =>

  val delay = 200

  var collections: Seq[Collection] = Seq.empty

  def initialize(indexColor: Int, icon: String, position: Int, isStateChanged: Boolean): Unit = {
    actions.initialize(indexColor, icon, isStateChanged).run
    Task.fork(di.collectionProcess.getCollections.run).resolveAsyncUi(
      onResult = (collections: Seq[Collection]) => actions.showCollections(collections, position),
      onException = (ex: Throwable) => actions.showContactUsError
    )
  }

  def resume(): Unit = di.observerRegister.registerObserver()

  def pause(): Unit = di.observerRegister.unregisterObserver()

  def back(): Unit = actions.back().run

  def destroy(): Unit = actions.destroy().run

  def resetAction(): Unit = actions.resetAction.run

  def destroyAction(): Unit = actions.destroyAction.run

  def reloadCards(reloadFragment: Boolean): Unit = actions.getCurrentCollection foreach { collection =>
    Task.fork(di.collectionProcess.getCollectionById(collection.id).run).resolveAsync(
      onResult = (c) => c map (newCollection => if (newCollection.cards != collection.cards) {
        actions.reloadCards(newCollection.cards, reloadFragment).run
      })
    )
  }

  def showMessageNotImplemented(): Unit = actions.showMessageNotImplemented.run

  def addCards(cards: Seq[AddCardRequest]): Unit = actions.getCurrentCollection foreach { collection =>
    Task.fork(di.collectionProcess.addCards(collection.id, cards).run).resolveAsyncUi(
      onResult = actions.addCards
    )
  }

  def removeCard(card: Card): Unit = actions.getCurrentCollection foreach { collection =>
    Task.fork(di.collectionProcess.deleteCard(collection.id, card.id).run).resolveAsyncUi(
      onResult = (_) => actions.removeCards(card)
    )
  }

  def addShortcut(collectionId: Int, name: String, shortcutIntent: Intent, bitmap: Option[Bitmap]): Unit = {
    Task.fork(createShortcut(collectionId, name, shortcutIntent, bitmap).run).resolveAsyncUi(
      onResult = actions.addCards
    )
  }

  def scrollY(scroll: Int, dy: Int): Unit = actions.translationScrollY(scroll).run

  def openReorderMode(current: ScrollType, canScroll: Boolean): Unit = actions.openReorderModeUi(current, canScroll).run

  def scrollType(sType: ScrollType): Unit = actions.notifyScroll(sType).run

  def emptyCollection(): Unit = actions.getCurrentCollection foreach { collection =>
    actions.showMenuButton(autoHide = false, collection).run
  }

  def firstItemInCollection(): Unit = actions.hideMenuButton.run

  def pullToClose(scroll: Int, scrollType: ScrollType, close: Boolean): Unit =
    actions.pullCloseScrollY(scroll, scrollType, close).run

  def close(): Unit = actions.exitTransition.run

  def startScroll(): Unit = actions.getCurrentCollection foreach { collection =>
    actions.showMenuButton(autoHide = true, collection).run
  }

  private[this] def createShortcut(collectionId: Int, name: String, shortcutIntent: Intent, bitmap: Option[Bitmap]):
  ServiceDef2[Seq[Card], ShortcutException with CardException] = for {
    path <- saveShortcutIcon(bitmap)
    addCardRequest = AddCardRequest(
      term = name,
      packageName = None,
      cardType = ShortcutCardType,
      intent = toNineCardIntent(shortcutIntent),
      imagePath = path)
    cards <- di.collectionProcess.addCards(collectionId, Seq(addCardRequest))
  } yield cards

  private[this] def saveShortcutIcon(bitmap: Option[Bitmap]):
  ServiceDef2[String, ShortcutException] = bitmap map { b =>
    di.deviceProcess.saveShortcutIcon(Random.nextString(10), b) // Name is not important here
  } getOrElse Service(Task(Result.answer(""))) // We use a empty string because the UI will generate an image

}

trait CollectionsUiActions {

  def initialize(indexColor: Int, icon: String, isStateChanged: Boolean): Ui[Any]

  def back(): Ui[Any]

  def destroy(): Ui[Any]

  def resetAction: Ui[Any]

  def destroyAction: Ui[Any]

  def showContactUsError: Ui[Any]

  def showMessageNotImplemented: Ui[Any]

  def showCollections(collections: Seq[Collection], position: Int): Ui[Any]

  def reloadCards(cards: Seq[Card], reloadFragments: Boolean): Ui[Any]

  def addCards(cards: Seq[Card]): Ui[Any]

  def removeCards(card: Card): Ui[Any]

  def getCurrentCollection: Option[Collection]

  def translationScrollY(scroll: Int): Ui[_]

  def openReorderModeUi(current: ScrollType, canScroll: Boolean): Ui[_]

  def notifyScroll(sType: ScrollType): Ui[_]

  def pullCloseScrollY(scroll: Int, scrollType: ScrollType, close: Boolean): Ui[_]

  def exitTransition: Ui[Any]

  def showMenuButton(autoHide: Boolean = true, collection: Collection): Ui[Any]

  def hideMenuButton: Ui[Any]
}
import org.specs2.mutable.Specification
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import scala.concurrent.duration._
import scala.concurrent.Await
import service.DocumentTypeDao
import MongoDBTestUtils.withMongoDb
import org.specs2.time.NoTimeConversions

object DocumentTypeSpec extends Specification with NoTimeConversions {

 /* "DocumentType" should {

    "be found in DB after being saved" in withMongoDb { implicit app =>
      val single1 = SingleLineText("Headline", "Hallo Welt")
      val single2 = SingleLineText("Subheadline", "wie gehts?")
      val html1 = HtmlText("Main Content", "<div><p>Seppl paragraph</p></div>")
      val html2 = HtmlText("Footer", "<div><p>Footer</p></div>")
      val html3 = HtmlText("htmlContent", "<div><p>HTML content</p></div>")
      val doc = DocumentType("Rob", "Article", List(), List(single1, single2), List(html2, html1, html3))
      val time = System.currentTimeMillis()
      val futureResult = DocumentTypeDao.save(doc)
      val res = Await.result(futureResult, 5 seconds)
      Await
      val foundDoc = Await.result(DocumentTypeDao.findById(doc._id), 5 seconds)
      foundDoc.name must_== doc.name
      foundDoc.createdAt must_== res.createdAt
      res.modifiedAt must >(time)
    }
    
    "be updated in DB after update is called" in withMongoDb {implicit app =>
      val single1 = SingleLineText("Headline", "Hallo Welt")
      val single2 = SingleLineText("Subheadline", "wie gehts?")
      val html1 = HtmlText("Main Content", "<div><p>Seppl paragraph</p></div>")
      val html2 = HtmlText("Footer", "<div><p>Footer</p></div>")
      val html3 = HtmlText("htmlContent", "<div><p>HTML content</p></div>")
      val doc = DocumentType("Rob", "Article", List(), List(single1, single2), List(html2, html1))

      val res = Await.result(DocumentTypeDao.save(doc), 5 seconds)
      
      val docInserted = Await.result(DocumentTypeDao.findById(doc._id), 5 seconds)
      
      val htmlTextElemsNew = docInserted.htmlTextElems.+:(html3)
      Await.result(DocumentTypeDao.update(docInserted.copy(htmlTextElems=htmlTextElemsNew)), 5 seconds)
      
      val foundDoc = Await.result(DocumentTypeDao.findById(doc._id), 5 seconds)
      //foundDoc.htmlTextElems.size must_== 3
      foundDoc.modifiedAt must >(foundDoc.createdAt)
    }
  }*/
}

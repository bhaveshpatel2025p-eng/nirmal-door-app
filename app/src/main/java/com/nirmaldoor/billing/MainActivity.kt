package com.nirmaldoor.billing

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class Door(var name:String="",var wft:Double=0.0,var win:Double=0.0,var hft:Double=0.0,var hin:Double=0.0,var qty:Double=1.0,var rate:Double=0.0){
    fun sqft()=(wft+win/12)*(hft+hin/12)
    fun amount()=sqft()*qty*rate
}
class MainActivity:Activity(){
    lateinit var inv:EditText; lateinit var customer:EditText; lateinit var mobile:EditText; lateinit var address:EditText
    lateinit var discount:EditText; lateinit var gst:EditText; lateinit var paid:EditText; lateinit var container:LinearLayout; lateinit var summary:TextView
    val doors=mutableListOf<Door>()
    override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main)
        inv=findViewById(R.id.invoiceNo);customer=findViewById(R.id.customer);mobile=findViewById(R.id.mobile);address=findViewById(R.id.address)
        discount=findViewById(R.id.discount);gst=findViewById(R.id.gst);paid=findViewById(R.id.paid);container=findViewById(R.id.itemsContainer);summary=findViewById(R.id.summary)
        newBill()
        findViewById<Button>(R.id.addDoor).setOnClickListener{doors.add(Door());render()}
        findViewById<Button>(R.id.save).setOnClickListener{saveBill()}
        findViewById<Button>(R.id.pdf).setOnClickListener{pdf()}
        findViewById<Button>(R.id.whatsapp).setOnClickListener{whatsapp()}
        findViewById<Button>(R.id.newBill).setOnClickListener{newBill()}
        listOf(discount,gst,paid).forEach{it.watch{calc()}}
    }
    fun newBill(){inv.setText("ND-${SimpleDateFormat("yyyy",Locale.US).format(Date())}-${System.currentTimeMillis().toString().takeLast(6)}");customer.setText("");mobile.setText("");address.setText("");discount.setText("0");gst.setText("0");paid.setText("0");doors.clear();doors.add(Door());doors.add(Door());render()}
    fun render(){container.removeAllViews();doors.forEachIndexed{idx,d->
        val box=LinearLayout(this);box.orientation=LinearLayout.VERTICAL;box.setPadding(8,8,8,8)
        fun field(h:String,v:String,change:(Double)->Unit):EditText{val e=EditText(this);e.hint=h;e.setText(v);e.inputType=2;e.watch{change(it.toDoubleOrNull()?:0.0);calc()};return e}
        val name=EditText(this);name.hint="Door / Item";name.setText(d.name);name.watch{d.name=it}
        box.addView(name);box.addView(field("Width feet",d.wft.toString()){d.wft=it});box.addView(field("Width inches",d.win.toString()){d.win=it});box.addView(field("Height feet",d.hft.toString()){d.hft=it});box.addView(field("Height inches",d.hin.toString()){d.hin=it});box.addView(field("Quantity",d.qty.toString()){d.qty=it});box.addView(field("Rate / Sq.Ft",d.rate.toString()){d.rate=it})
        val out=TextView(this);out.textSize=16f;out.setPadding(4,6,4,6);box.addView(out);val rm=Button(this);rm.text="REMOVE DOOR";rm.setOnClickListener{doors.removeAt(idx);render()};box.addView(rm);container.addView(box)
    };calc()}
    fun calc(){val sub=doors.sumOf{it.amount()};val dis=discount.text.toString().toDoubleOrNull()?:0.0;val taxable=(sub-dis).coerceAtLeast(0.0);val tax=taxable*(gst.text.toString().toDoubleOrNull()?:0.0)/100;val total=taxable+tax;val p=paid.text.toString().toDoubleOrNull()?:0.0;val due=(total-p).coerceAtLeast(0.0);summary.text="Subtotal: ${money(sub)}\nDiscount: ${money(dis)}\nGST: ${money(tax)}\nGrand Total: ${money(total)}\nPaid: ${money(p)}\nBalance Due: ${money(due)}";for(i in 0 until container.childCount){val box=container.getChildAt(i) as LinearLayout;if(box.childCount>7)(box.getChildAt(7) as TextView).text="Sq.Ft: %.2f | Amount: %s".format(doors[i].sqft(),money(doors[i].amount()))}}
    fun money(x:Double)=String.format(Locale.US,"₹%,.2f",x)
    fun text():String{val sub=doors.sumOf{it.amount()};val dis=discount.text.toString().toDoubleOrNull()?:0.0;val taxable=(sub-dis).coerceAtLeast(0.0);val tax=taxable*(gst.text.toString().toDoubleOrNull()?:0.0)/100;val total=taxable+tax;val p=paid.text.toString().toDoubleOrNull()?:0.0;val due=(total-p).coerceAtLeast(0.0);return buildString{append("NIRMAL DOOR\nOpp Railway Station, KUDACHI-591311\nMobile: 8050304196\nEmail: nirmaldoorkud@gmail.com\n\nInvoice: ${inv.text}\nCustomer: ${customer.text}\nMobile: ${mobile.text}\nAddress: ${address.text}\n\n");doors.forEachIndexed{i,d->append("${i+1}. ${d.name} | %.2f sq.ft x %.0f @ %.2f = %.2f\n".format(d.sqft(),d.qty,d.rate,d.amount()))};append("\nGrand Total: %.2f\nPaid: %.2f\nBalance Due: %.2f".format(total,p,due))}}
    fun saveBill(){getSharedPreferences("bills",0).edit().putString(inv.text.toString(),text()).apply();Toast.makeText(this,"Bill saved on phone",Toast.LENGTH_SHORT).show()}
    fun pdf(){val doc=PdfDocument();val page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,1).create());val paint=Paint();paint.textSize=12f;var y=35f;text().split("\n").forEach{page.canvas.drawText(it.take(90),25f,y,paint);y+=18};doc.finishPage(page);val f=File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),"${inv.text}.pdf");FileOutputStream(f).use{doc.writeTo(it)};doc.close();Toast.makeText(this,"PDF saved: ${f.name}",Toast.LENGTH_LONG).show()}
    fun whatsapp(){val i=Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:${mobile.text}"));i.setPackage("com.whatsapp");i.putExtra("sms_body",text());try{startActivity(i)}catch(e:Exception){startActivity(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,text())})}}
    fun EditText.watch(block:(String)->Unit){addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,a:Int,c:Int,d:Int){};override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){block(s?.toString()?:"")};override fun afterTextChanged(e:Editable?){} })}
}

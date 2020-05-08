package android.inflabnet.mytest.fragments


import android.content.Context
import android.content.Intent
import android.inflabnet.mytest.database.adapter.GastosAdapter
import android.inflabnet.mytest.R
import android.inflabnet.mytest.database.database.AppDatabase
import android.inflabnet.mytest.database.database.AppDatabaseService
import android.inflabnet.mytest.database.model.MesaOrc
import android.inflabnet.mytest.database.model.Orcamento
import android.inflabnet.mytest.database.model.OrcamentoEMesa
import android.inflabnet.mytest.maps.HomeMapsActivity
import android.inflabnet.mytest.maps.MapsActivity
import android.inflabnet.mytest.mesas.activity.MesaActivity
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.alert_orcamento.view.*
import kotlinx.android.synthetic.main.fragment_home_menu.*
import java.util.*

/**
 * A simple [Fragment] subclass.
 */
class HomeMenuFragment : Fragment() {


    private lateinit var appDatabase : AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val contFrag = activity!!.applicationContext
        appDatabase = AppDatabaseService.getInstance(contFrag)

        //limpar tabela orçamento
        //DeleteOrcamento().execute()
        //limpar tabela MesaOrc
        //DeleteMesaGastos().execute()
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home_menu, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //quanto já gastou no presente mês
        val jaGastou = GetGastosAtuais().execute(currentMonth2()).get()
        txtGastou.text = jaGastou.toString()

        //listar gastos na list
        val checkGastos: Int = GetRowsGastos().execute().get()

        if (checkGastos != 0) {
            listarGastos()
        }

        //colocar o valor do orçamento na tela se não tiver orçamento para o mês corrente
        val totRows = GetRows().execute(currentMonth2())
        if (totRows.get() != 0){
            txtOrcamentoAtual.text = GetOrcamento().execute(currentMonth2()).get().valor.toString()
            //colocar cor nos gastos atuais de acordo com orçamento
            if(!jaGastou.equals(0)) {
                val orcamento = GetOrcamento().execute(currentMonth2()).get().valor
                val percentage = (jaGastou / orcamento) * 100.0
                //Toast.makeText(this.context!!.applicationContext,"Percentagem: ${percentage}",Toast.LENGTH_SHORT).show()
                //Toast.makeText(this.context!!.applicationContext,"orcamento: ${orcamento}",Toast.LENGTH_SHORT).show()
                corOrcamento(percentage)
            }

        }else{//já pergunta o orçamento caso seja o primeiro acesso
            //apresentar o layout de pergunta
            val mDialogView = LayoutInflater.from(activity!!).inflate(R.layout.alert_orcamento, null)
            //builder do alertdialog com itens da comanda e membros para dividir (Pergunta)
            val mBuilder = AlertDialog.Builder(activity!!)
                .setView(mDialogView)
                .setTitle("Inserir Orçamento para ${currentMonth2()}")
            //verificar leak memory
            val  mAlertDialog = mBuilder.show()

            mDialogView.btnPerguntar.setOnClickListener {
                val orcamentoNew = mDialogView.edtOrcamento.text
                InsertOrcamento().execute(Orcamento(null,currentMonth2(),orcamentoNew.toString().toInt()))
                txtOrcamentoAtual.setText(orcamentoNew.toString())
                mAlertDialog.dismiss()
            }
            mDialogView.btnCancelar.setOnClickListener {
                mAlertDialog.dismiss()
            }

        }

        val mAtual = currentMonth2()

        btnMapsMenu.setOnClickListener {
            val intt = Intent(this.context!!.applicationContext, HomeMapsActivity::class.java)
            startActivity(intt)
        }

        btnMesas.setOnClickListener {
            val intt = Intent(this.context!!.applicationContext, MesaActivity::class.java)
            startActivity(intt)
        }

        //inserir novo orçamento mensal
        btnOrcamento.setOnClickListener {
            val contFrag = activity!!.applicationContext
            if(edtOrc.text.isNullOrBlank()){
                Toast.makeText(this.context!!.applicationContext,"Valor inválido!", Toast.LENGTH_SHORT).show()
            }else{
                //não há orçamentos - primeiro acesso
                if (totRows.get() == 0){
                    val novoOrcamento = edtOrc.text.toString()
                    InsertOrcamento().execute(Orcamento(null,currentMonth2(),novoOrcamento.toInt()))
                    Toast.makeText(this.context!!.applicationContext,"Novo orçamento ${totRows.get()}",Toast.LENGTH_SHORT).show()
                }else {
                    txtOrcamentoAtual.text =  GetOrcamento().execute(mAtual).get().valor.toString()
                    val orcamentoAtual = GetOrcamento().execute(currentMonth2()).get()
                    val txtTitulo = "Trocar Orçamento"
                    val dialogBuilder = AlertDialog.Builder(activity!!)
                    dialogBuilder.setMessage("Tem certeza que gostaria de trocar o orçamento ${orcamentoAtual.valor} por ${edtOrc.text} ?")
                        .setCancelable(false)
                        .setPositiveButton("Sim") { _, _ ->
                            //segue a troca de orçamentos
                            val novoOrcamento = edtOrc.text.toString()
                            orcamentoAtual.valor = novoOrcamento.toInt()
                            UpdateOrcamento().execute(orcamentoAtual)
                            edtOrc.setText("")
//                            edtOrc.isFocusable = false;
//                            edtOrc.isFocusableInTouchMode = false;
//                            edtOrc.isClickable = false;
//                            edtOrc.visibility = View.INVISIBLE
//                            btnOrcamento.visibility = View.INVISIBLE
                            txtOrcamentoAtual.setText(orcamentoAtual.valor.toString())
                            // Hide the keyboard.
                            val inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputMethodManager.hideSoftInputFromWindow(btnOrcamento.windowToken, 0)
                        }
                        .setNeutralButton("Cancelar") { _, _ ->
                            Toast.makeText(contFrag, "Operação cancelada", Toast.LENGTH_SHORT).show()
                        }
                    val alert = dialogBuilder.create()
                    alert.setTitle(txtTitulo)
                    alert.show()
                }
            }
            val orcAtual = GetOrcamento().execute(currentMonth2()).get().valor
            //Toast.makeText(this.context!!.applicationContext, "Orçamento é ${orcAtual}", Toast.LENGTH_SHORT).show()
        }

        textView2.setOnClickListener {
            ListaGastos().execute()
            //quanto já gastou no presente mês
            val jaGastou = GetGastosAtuais().execute(currentMonth2()).get()
            txtGastou.text = jaGastou.toString()
            val orcamento = GetOrcamento().execute(currentMonth2()).get().valor
            val percentage = (jaGastou / orcamento) * 100.0
//            Toast.makeText(this.context!!.applicationContext,"Percentagem: ${percentage}",Toast.LENGTH_SHORT).show()
//            Toast.makeText(this.context!!.applicationContext,"orcamento: ${orcamento}",Toast.LENGTH_SHORT).show()
//            Toast.makeText(this.context!!.applicationContext,"Ja Gastou: ${jaGastou}",Toast.LENGTH_SHORT).show()
            corOrcamento(percentage)
            //Log.i("TESTEEE",jaGastou.toString())
        }
    }

    private fun corOrcamento(percentage: Double) {
        if (percentage < 75.0) {
            txtOrcamentoAtual.setTextColor(ContextCompat.getColor(activity!!, R.color.green))
            txtGastou.setTextColor(ContextCompat.getColor(activity!!, R.color.green))
        } else if (percentage < 90.0) {
            txtOrcamentoAtual.setTextColor(ContextCompat.getColor(activity!!, R.color.yellow))
            txtGastou.setTextColor(ContextCompat.getColor(activity!!, R.color.yellow))
        } else {
            txtOrcamentoAtual.setTextColor(ContextCompat.getColor(activity!!, R.color.red))
            txtGastou.setTextColor(ContextCompat.getColor(activity!!, R.color.red))
        }
    }


    private fun listarGastos() {
        ListaGastos().execute()
    }

    inner class ListaGastos:AsyncTask<Unit,Unit,List<OrcamentoEMesa>>(){

        override fun doInBackground(vararg params: Unit):List<OrcamentoEMesa> {
            val gastos = appDatabase.orcamentoDAO().buscaGeral()
            return gastos
        }

        override fun onPostExecute(result: List<OrcamentoEMesa>) {
            val toReturn : MutableList<MesaOrc> = mutableListOf()
            result.forEach {
                it.mesaorc.forEach {
                    toReturn.add(it)
                }
            }

            val linearLayoutManager = LinearLayoutManager(activity!!.applicationContext)
            rvGastos.layoutManager = linearLayoutManager
            rvGastos.scrollToPosition(result!!.size)
            rvGastos.adapter =
                GastosAdapter(toReturn) {

                }

            result.forEach {
                Log.i("RESULT ORC", it.orcamento.toString())
                it.mesaorc.forEach {
                    Log.i("RESULT MES", " ${it.nome_mesa} ${it.gasto} ")
                }
            }

        }
    }

    inner class UpdateOrcamento:AsyncTask<Orcamento,Unit,Unit>(){
        override fun doInBackground(vararg params: Orcamento?) {
            appDatabase.orcamentoDAO().atualiza(params[0]!!)
        }
    }

    inner class GetRows:AsyncTask<String,Unit,Int>(){
        override fun doInBackground(vararg params: String?):Int {
            val totRows = appDatabase.orcamentoDAO().getNumRows(params[0]!!)
            return totRows
        }
    }

    inner class GetRowsGastos:AsyncTask<Int,Unit,Int>(){
        override fun doInBackground(vararg params: Int?):Int {
            val totRows = appDatabase.mesaDAO().getNumRows()
            return totRows
        }
    }

    inner class DeleteOrcamento:AsyncTask<Unit,Unit,Unit>(){
        override fun doInBackground(vararg params: Unit?) {
            appDatabase.orcamentoDAO().delete()
        }
    }

    inner class DeleteMesaGastos:AsyncTask<Unit,Unit,Unit>(){
        override fun doInBackground(vararg params: Unit?) {
            appDatabase.mesaDAO().delete()
        }
    }

    inner class InsertOrcamento:AsyncTask<Orcamento,Unit,Unit>(){
        override fun doInBackground(vararg params: Orcamento?) {
            appDatabase.orcamentoDAO().guardar(params[0]!!)
        }

    }

    inner class GetOrcamento:AsyncTask<String,Unit,Orcamento>(){
        override fun doInBackground(vararg params: String?): Orcamento? {
            val valorMesdb = appDatabase.orcamentoDAO().buscarMes(params[0]!!)
            return valorMesdb
        }

    }

    inner class GetGastosAtuais:AsyncTask<String,Unit,Int>(){
        override fun doInBackground(vararg params: String?): Int? {
            val valorGasto = appDatabase.mesaDAO().gastosAtuais(params[0]!!)
            return valorGasto
        }

    }

    private fun currentMonth2():String {
        val month: Calendar = Calendar.getInstance()
        val mesAtual = month.get(Calendar.MONTH)
        var mes = when (mesAtual) {
            0 -> "Janeiro"
            1 -> "Fevereiro"
            2 ->  "Março"
            3 ->  "Abril"
            4 -> "Maio"
            5 -> "Junho"
            6 -> "Julho"
            7 ->  "Agosto"
            8 ->  "Setembro"
            9 ->  "Outubro"
            10 -> "Novembro"
            11 ->  "Dezembro"
            else -> "Mês"
        }
        txtMes.setText(mes)
        return mes
    }
}

package br.unit.eleicao.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Emendas ao Orçamento da União indicadas por um parlamentar (Portal da Transparência / CGU):
 * quanto foi reservado (empenhado), quanto chegou a ser pago e para onde foi.
 */
public class ResumoEmendas {

    private final String autor;
    private double empenhado;
    private double pago;
    private double pagoTransferenciaEspecial;
    private int quantidade;
    private final Map<Integer, Double> pagoPorAno = new TreeMap<>();
    private final Map<String, Double> pagoPorLocal = new LinkedHashMap<>();
    private final Map<String, Double> pagoPorArea = new LinkedHashMap<>();

    public ResumoEmendas(String autor) {
        this.autor = autor;
    }

    /** Acrescenta uma linha do arquivo de emendas. */
    public void somar(int ano, String local, String area, double valorEmpenhado, double valorPago,
                      boolean transferenciaEspecial) {
        quantidade++;
        empenhado += valorEmpenhado;
        pago += valorPago;
        if (transferenciaEspecial) {
            pagoTransferenciaEspecial += valorPago;
        }
        pagoPorAno.merge(ano, valorPago, Double::sum);
        pagoPorLocal.merge(local == null || local.isBlank() ? "Não informado" : local, valorPago, Double::sum);
        pagoPorArea.merge(area == null || area.isBlank() ? "Não informada" : area, valorPago, Double::sum);
    }

    /** Restaura um total já agregado (leitura da base processada). */
    public void restaurar(double empenhado, double pago, double pagoTransferenciaEspecial, int quantidade) {
        this.empenhado = empenhado;
        this.pago = pago;
        this.pagoTransferenciaEspecial = pagoTransferenciaEspecial;
        this.quantidade = quantidade;
    }

    public void restaurarAno(int ano, double valor) {
        pagoPorAno.put(ano, valor);
    }

    public void restaurarLocal(String local, double valor) {
        pagoPorLocal.put(local, valor);
    }

    public void restaurarArea(String area, double valor) {
        pagoPorArea.put(area, valor);
    }

    /** Os n maiores itens de um mapa de valores, do maior para o menor. */
    public static List<Map.Entry<String, Double>> maiores(Map<String, Double> mapa, int n) {
        List<Map.Entry<String, Double>> lista = new ArrayList<>(mapa.entrySet());
        lista.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return lista.subList(0, Math.min(n, lista.size()));
    }

    public String getAutor() {
        return autor;
    }

    public double getEmpenhado() {
        return empenhado;
    }

    public double getPago() {
        return pago;
    }

    /** Parte paga por "transferência especial" (as chamadas emendas Pix, com rastreio mais difícil). */
    public double getPagoTransferenciaEspecial() {
        return pagoTransferenciaEspecial;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public Map<Integer, Double> getPagoPorAno() {
        return Collections.unmodifiableMap(pagoPorAno);
    }

    public Map<String, Double> getPagoPorLocal() {
        return Collections.unmodifiableMap(pagoPorLocal);
    }

    public Map<String, Double> getPagoPorArea() {
        return Collections.unmodifiableMap(pagoPorArea);
    }
}

package com.comandago.api.pedido.service;

import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.pedido.dto.request.CambioInsumoRequest;
import com.comandago.api.pedido.dto.request.DetallePedidoItemRequest;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.entity.ProductoInsumo;
import com.comandago.api.producto.enums.TipoProducto;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.receta.entity.Receta;
import com.comandago.api.receta.entity.RecetaIngrediente;
import com.comandago.api.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetalleComposicionHelperTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private DetalleComposicionHelper helper;

    @Test
    void cargarProducto_insumoInternoSinCategoria_lanzaBusinessException() {
        Producto insumo = new Producto();
        insumo.setId(1L);
        insumo.setNombre("Cebolla");
        insumo.setTipo(TipoProducto.INSUMO);
        insumo.setActivo(true);
        insumo.setDisponible(true);
        insumo.setCategoria(null);
        when(productoRepository.findByIdWithComposicion(1L)).thenReturn(Optional.of(insumo));

        assertThatThrownBy(() -> helper.cargarProductoParaPedido(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("uso interno");
    }

    @Test
    void cargarProducto_insumoVendibleConCategoria_ok() {
        Categoria cat = Categoria.builder().id(5L).nombre("Porciones").build();
        Producto insumo = new Producto();
        insumo.setId(2L);
        insumo.setNombre("Porción frijoles");
        insumo.setTipo(TipoProducto.INSUMO);
        insumo.setActivo(true);
        insumo.setDisponible(true);
        insumo.setCategoria(cat);
        when(productoRepository.findByIdWithComposicion(2L)).thenReturn(Optional.of(insumo));

        Producto loaded = helper.cargarProductoParaPedido(2L);

        assertThat(loaded.getId()).isEqualTo(2L);
    }

    @Test
    void resolverLinea_compuestoLegacyConExtra_sumaPrecioYGeneraNotas() {
        Producto compuesto = compuestoLegacyConTocineta();
        DetallePedidoItemRequest item = new DetallePedidoItemRequest();
        item.setProductoId(10L);
        item.setCantidad(1);
        item.setExtrasIds(List.of(20L));
        item.setRemovidosIds(List.of(30L));
        item.setPrecioUnitario(new BigDecimal("15500.00"));

        var result = helper.resolverLinea(compuesto, item, new BigDecimal("12000"));

        assertThat(result.precioUnitario()).isEqualByComparingTo("15500.00");
        assertThat(result.notasPreparacion()).contains("SIN: Cebolla");
        assertThat(result.notasPreparacion()).contains("Tocineta");
    }

    @Test
    void resolverLinea_precioIncorrecto_lanzaBusinessException() {
        Producto compuesto = compuestoLegacyConTocineta();
        DetallePedidoItemRequest item = new DetallePedidoItemRequest();
        item.setExtrasIds(List.of(20L));
        item.setPrecioUnitario(new BigDecimal("12000"));

        assertThatThrownBy(() -> helper.resolverLinea(compuesto, item, new BigDecimal("12000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("precio unitario");
    }

    @Test
    void resolverLinea_recetaConCambioValido_generaNotaSinCambiarPrecio() {
        Producto compuesto = compuestoConReceta();
        Producto frijoles = frijolesDisponible();
        when(productoRepository.findById(21L)).thenReturn(Optional.of(frijoles));

        DetallePedidoItemRequest item = new DetallePedidoItemRequest();
        item.setProductoId(10L);
        item.setCantidad(1);
        CambioInsumoRequest cambio = new CambioInsumoRequest();
        cambio.setDesdeProductoId(20L);
        cambio.setHaciaProductoId(21L);
        item.setCambios(List.of(cambio));
        item.setPrecioUnitario(new BigDecimal("12000.00"));

        var result = helper.resolverLinea(compuesto, item, new BigDecimal("12000"));

        assertThat(result.precioUnitario()).isEqualByComparingTo("12000.00");
        assertThat(result.notasPreparacion()).isEqualTo("CAMBIO: Lentejas → Frijoles");
    }

    @Test
    void resolverLinea_recetaCambioYRemovido_ok() {
        Producto compuesto = compuestoConReceta();
        Producto frijoles = frijolesDisponible();
        when(productoRepository.findById(21L)).thenReturn(Optional.of(frijoles));

        DetallePedidoItemRequest item = new DetallePedidoItemRequest();
        CambioInsumoRequest cambio = new CambioInsumoRequest();
        cambio.setDesdeProductoId(20L);
        cambio.setHaciaProductoId(21L);
        item.setCambios(List.of(cambio));
        item.setRemovidosIds(List.of(30L));
        item.setPrecioUnitario(new BigDecimal("12000.00"));

        var result = helper.resolverLinea(compuesto, item, new BigDecimal("12000"));

        assertThat(result.notasPreparacion()).contains("CAMBIO: Lentejas → Frijoles");
        assertThat(result.notasPreparacion()).contains("SIN: Papa frita");
    }

    @Test
    void resolverLinea_recetaRemoverYCambiarMismo_lanza() {
        Producto compuesto = compuestoConReceta();
        DetallePedidoItemRequest item = new DetallePedidoItemRequest();
        CambioInsumoRequest cambio = new CambioInsumoRequest();
        cambio.setDesdeProductoId(20L);
        cambio.setHaciaProductoId(21L);
        item.setCambios(List.of(cambio));
        item.setRemovidosIds(List.of(20L));

        assertThatThrownBy(() -> helper.resolverLinea(compuesto, item, new BigDecimal("12000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("remover y cambiar");
    }

    @Test
    void resolverLinea_recetaCambioNoDisponible_lanza() {
        Producto compuesto = compuestoConReceta();
        Producto frijoles = frijolesDisponible();
        frijoles.setDisponible(false);
        when(productoRepository.findById(21L)).thenReturn(Optional.of(frijoles));

        DetallePedidoItemRequest item = new DetallePedidoItemRequest();
        CambioInsumoRequest cambio = new CambioInsumoRequest();
        cambio.setDesdeProductoId(20L);
        cambio.setHaciaProductoId(21L);
        item.setCambios(List.of(cambio));

        assertThatThrownBy(() -> helper.resolverLinea(compuesto, item, new BigDecimal("12000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no está disponible");
    }

    @Test
    void resolverLinea_recetaCambioFijo_lanza() {
        Producto compuesto = compuestoConReceta();
        DetallePedidoItemRequest item = new DetallePedidoItemRequest();
        CambioInsumoRequest cambio = new CambioInsumoRequest();
        cambio.setDesdeProductoId(40L);
        cambio.setHaciaProductoId(21L);
        item.setCambios(List.of(cambio));

        assertThatThrownBy(() -> helper.resolverLinea(compuesto, item, new BigDecimal("12000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no es válido o no es removible");
    }

    @Test
    void resolverLinea_recetaExtras_lanza() {
        Producto compuesto = compuestoConReceta();
        DetallePedidoItemRequest item = new DetallePedidoItemRequest();
        item.setExtrasIds(List.of(99L));

        assertThatThrownBy(() -> helper.resolverLinea(compuesto, item, new BigDecimal("12000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no admite extras");
    }

    private Producto compuestoLegacyConTocineta() {
        Producto compuesto = new Producto();
        compuesto.setId(10L);
        compuesto.setTipo(TipoProducto.COMPUESTO);
        compuesto.setNombre("Perro");
        compuesto.setComposicion(new ArrayList<>());

        Producto tocineta = new Producto();
        tocineta.setId(20L);
        tocineta.setNombre("Tocineta");
        tocineta.setTipo(TipoProducto.INSUMO);

        ProductoInsumo extra = new ProductoInsumo();
        extra.setProductoCompuesto(compuesto);
        extra.setProductoInsumo(tocineta);
        extra.setEsExtra(true);
        extra.setEsRemovible(false);
        extra.setPrecioExtra(new BigDecimal("3500"));
        compuesto.getComposicion().add(extra);

        Producto cebolla = new Producto();
        cebolla.setId(30L);
        cebolla.setNombre("Cebolla");
        cebolla.setTipo(TipoProducto.INSUMO);

        ProductoInsumo removible = new ProductoInsumo();
        removible.setProductoCompuesto(compuesto);
        removible.setProductoInsumo(cebolla);
        removible.setEsExtra(false);
        removible.setEsRemovible(true);
        compuesto.getComposicion().add(removible);

        return compuesto;
    }

    private Producto compuestoConReceta() {
        Categoria granos = Categoria.builder().id(7L).nombre("Porciones de granos").build();
        Categoria bastimento = Categoria.builder().id(8L).nombre("Porciones de bastimento").build();
        Categoria proteina = Categoria.builder().id(3L).nombre("Porciones de proteína").build();

        Producto lentejas = new Producto();
        lentejas.setId(20L);
        lentejas.setNombre("Lentejas");
        lentejas.setTipo(TipoProducto.INSUMO);
        lentejas.setCategoria(granos);

        Producto papa = new Producto();
        papa.setId(30L);
        papa.setNombre("Papa frita");
        papa.setTipo(TipoProducto.INSUMO);
        papa.setCategoria(bastimento);

        Producto cerdo = new Producto();
        cerdo.setId(40L);
        cerdo.setNombre("Cerdo");
        cerdo.setTipo(TipoProducto.INSUMO);
        cerdo.setCategoria(proteina);

        Receta receta = new Receta();
        receta.setId(1L);
        receta.setIngredientes(new ArrayList<>());

        RecetaIngrediente riLentejas = new RecetaIngrediente();
        riLentejas.setProducto(lentejas);
        riLentejas.setEsRemovible(true);
        riLentejas.setOrden(0);
        receta.agregarIngrediente(riLentejas);

        RecetaIngrediente riPapa = new RecetaIngrediente();
        riPapa.setProducto(papa);
        riPapa.setEsRemovible(true);
        riPapa.setOrden(1);
        receta.agregarIngrediente(riPapa);

        RecetaIngrediente riCerdo = new RecetaIngrediente();
        riCerdo.setProducto(cerdo);
        riCerdo.setEsRemovible(false);
        riCerdo.setOrden(2);
        receta.agregarIngrediente(riCerdo);

        Producto compuesto = new Producto();
        compuesto.setId(10L);
        compuesto.setTipo(TipoProducto.COMPUESTO);
        compuesto.setNombre("Pechuga");
        compuesto.setReceta(receta);
        compuesto.setComposicion(new ArrayList<>());
        return compuesto;
    }

    private Producto frijolesDisponible() {
        Categoria granos = Categoria.builder().id(7L).nombre("Porciones de granos").build();
        Producto frijoles = new Producto();
        frijoles.setId(21L);
        frijoles.setNombre("Frijoles");
        frijoles.setTipo(TipoProducto.INSUMO);
        frijoles.setActivo(true);
        frijoles.setDisponible(true);
        frijoles.setCategoria(granos);
        return frijoles;
    }
}

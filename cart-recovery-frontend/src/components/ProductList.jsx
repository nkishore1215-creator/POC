import ProductCard from './ProductCard.jsx'

function ProductList({
  products = [],
  cartItems = [],
  loading = false,
  error = '',
  onAddToCart,
  addingProductId = null,
}) {
  if (loading) {
    return <p role="status">Loading products...</p>
  }

  if (error) {
    return <p role="alert">{error}</p>
  }

  if (products.length === 0) {
    return <p>No products are available.</p>
  }

  return (
    <section aria-labelledby="products-heading">
       
   

      <div className="product-list">
        {products.map((product) => {
          const productId = product.productId ?? product.id
          const quantityInCart = cartItems.reduce((total, item) => {
            const itemProductId =
              item.product?.productId ??
              item.product?.id ??
              item.productId

            return itemProductId === productId
              ? total + (Number(item.quantity) || 0)
              : total
          }, 0)

          return (
            <ProductCard
              key={productId}
              product={product}
              quantityInCart={quantityInCart}
              onAddToCart={onAddToCart}
              isAdding={addingProductId === productId}
            />
          )
        })}
      </div>
    </section>
  )
}

export default ProductList

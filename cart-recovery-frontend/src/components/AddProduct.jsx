import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { productApi } from "../services/api";


function AddProduct(){

    const navigate= useNavigate()
    const [error, setError]= useState('')
    const [saving, setSaving]=useState(false)
    const [product, setProduct]= useState({
        productName:'',
        description:'',
        price:'',
        stockQuantity:'',
        category:'MOBILE'
    })


    function handleChange(e){
      const {name, value}= e.target

      setProduct((currentProduct)=>({
        ...currentProduct,
        [name]:value
      }))
        
    }

    async function handleSubmit(e){
      e.preventDefault()
      setError('')
      setSaving(true)
try{

    const productRequest={
    productName:product.productName.trim(),
    description: product.description.trim(),
    price:Number(product.price),
    stockQuantity: Number(product.stockQuantity),
    category:product.category

    }

    await productApi.create(productRequest)
    alert("Product added successfuully")

    navigate('/')


}catch(error){
console.error("Add product error", error)
setError(error.message)
}finally{
    setSaving(false)
}

    }

    return(

        <main className="add-product-page">
            <div className="add-product-card">
          <h2>Add Product</h2><br></br>
          
            {
                error && (
                    <p className="error-meaasge">{error}</p>
                )
            }
         

         <form onSubmit={handleSubmit}>
            <div className="form-group">
                 <label>Product Name :</label>
                 <input type="text"
                placeholder="Enter the Name"
                onChange={handleChange}
                name="productName"
                value={product.productName}
                required></input>
                </div>
                  <div className="form-group">
                    <label>Description:</label>
                    <textarea 
                    name="description"
                    value={product.description}
                    onChange={handleChange}
                    placeholder="Enter product desciption"
                    rows="4"
                     ></textarea>
                  </div>
                
                <div className="form-group">
                   <label>Price:</label>
                <input type= "number"
                placeholder="Enter price"
                onChange={handleChange}
                name="price"
                min="0"
                steps="0.01"
                required
                value={product.price}
                required></input>
                </div>
                <div className="form-group">
                   <label>Stock Qunatity:</label>
                <input type="number"
                placeholder="Enter the stock"
                onChange={handleChange}
                name="stockQuantity"
                value={product.stockQuantity}
                required></input>
                </div>
                <div className="form-group">
                   <label>Category:</label>
                <select name="category"
               
                onChange={handleChange}
                name="category"
                value={product.category}
              >

                <option value="MOBILE"> Mobile</option>
                <option value="TV"> Tv</option>
                <option value="LAPTOP"> Laptop</option>
              </select>
                </div>

                <div className="form-actions">
                    <button type="button" className="cancel-button"
                    onClick={()=>navigate('/')}
                    >
                        Cancel
                    </button>
                    <button type="submit" className="save-button"
                    disabled={saving}
                    > {saving ? 'Saving' :'Save'}</button>
                </div>
                </form>   
            </div>
        </main>
    )

}

export default AddProduct
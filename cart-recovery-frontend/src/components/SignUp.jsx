import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { authApi } from "../services/api";



function SignUp(){

    const navigate=useNavigate()
    const[signup, setSignup]= useState(
        {
           firstName:"",
           lastName:"",
           email:"",
           phone:"",
           password:"",
           emailOptIn: false
        }
    )
    const[error, setError]=useState("")
    const[success,setSuccess]=useState("")
    const[loading, setLoading]=useState(false)


     function handleChange(e){
         const {name, value, type, checked}= e.target
          setSignup((current)=>({
               ...current,
            [name]: type === "checkbox" ? checked : value
          })

          )
     }

     async function handleSubmit(e){
        e.preventDefault()
        setError("")
        setSuccess("")

        try{
           
            setLoading(true)
            await authApi.signup({
                firstName:signup.firstName,
                lastName:signup.lastName,
                email:signup.email,
                password:signup.password,
                phone:signup.phone,
               emailOptIn:signup.emailOptIn

            })

             console.log(signup.firstName)
             
             console.log(signup.emailOpt)

            setSuccess("Account created successfully")
            setTimeout(()=>{
                navigate('/login')
            }, 1500)

        }catch(error){
            console.error("signup error", error)
           setError("Email already registered. Please use another email.")
        }finally{
            setLoading(false)
        }



     }

  return(
    <main className="add-product-page">
   <div className="add-product-card">
    <div className="login-header">
        <div className="login-logo">  🛒</div>
        <h1> Create an Account</h1>
        <p>Sign up for cart recovery store</p>
    </div>
      {error && (
        <p className="error-message">{error}</p>
      )}{
        success &&(
            <p className="success-message">{success}</p>
        )
      }

      <form onSubmit={handleSubmit}>
        <div className="form-group">
            <label>First Name:</label>
            <input type="text"
            name="firstName"
            placeholder="Enter Name"
            onChange={handleChange}
            value={signup.firstName}
            required></input>
        </div>
        <div className="form-group">
            <label>Last Name:</label>
            <input type="text"
            name="lastName"
            placeholder="Enter last name"
            onChange={handleChange}
            value={signup.lastName}
            required></input>
        </div>
        <div className="form-group">
            <label>Email:</label>
            <input type="email"
            name="email"
            placeholder="Enter Email"
            onChange={handleChange}
            value={signup.email}
            required></input>
        </div>
        <div className="form-group">
            <label>Password:</label>
            <input type="password"
            name="password"
            placeholder="Enter password"
            onChange={handleChange}
            value={signup.password}
            required></input>
        </div>
        <div className="form-group">
            <label>phone:</label>
            <input type="text"
            name="phone"
            placeholder="Enter Phone"
            value={signup.phone}
            onChange={handleChange}
            required></input>
        </div>
        <div className="form-group">
            <label>Email-Opt:</label>
            <input type="checkbox"
            name="emailOptIn"
            checked={signup.checked}
                      onChange={handleChange}
            
            ></input>
        </div>
         <div className="login-buttons">
             <button className="signup-button" type="button"
            onClick={()=> navigate('/login')}> Back</button>
            <button type="submit"
            className="signup-button"
            disabled={loading}
            >
                {loading ? 'Creating...' : "Sign Up"}
            </button>
           
         </div>

      </form>
   </div>

    </main>
  )   

}
export default SignUp
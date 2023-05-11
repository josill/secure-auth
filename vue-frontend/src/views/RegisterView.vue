<template>
    <div class="flex flex-col flex-1 justify-center items-center bg-slate-100 font-montserrat">
      <div class="relative border-2 border-gray-300 shadow rounded-lg max-w-[300px] md:max-w-[400px] flex flex-col w-full !p-6 3xl:p-![18px] bg-white">   
        <div class="relative flex flex-row justify-between">
            <h4 class="text-xl font-bold text-navy-700 mb-6">
                Register
            </h4>
        </div>
        <div class="mb-4">
            <label for="email" class="text-sm text-navy-700 ml-0.5">Email</label>
            <input type="text" id="email" placeholder="bob@gmail.com" class="mt-2 flex h-12 w-full items-center justify-center rounded-xl border bg-white/0 p-3 text-sm outline-none border-gray-200">
        </div>
        <div class="mb-4">
            <label for="first-name" class="text-sm text-navy-700 ml-0.5">First name</label>
            <input type="text" id="first-name" class="mt-2 flex h-12 w-full items-center justify-center rounded-xl border bg-white/0 p-3 text-sm outline-none border-gray-200">
        </div>
        <div class="mb-4">
            <label for="last-name" class="text-sm text-navy-700 ml-0.5">Last name</label>
            <input type="text" id="last-name" class="mt-2 flex h-12 w-full items-center justify-center rounded-xl border bg-white/0 p-3 text-sm outline-none border-gray-200">
        </div>
        <div class="mb-4 relative">
            <label for="password" class="text-sm text-navy-700 ml-0.5">Password</label>
            <input :type="passwordType1" id="password" class="mt-2 flex h-12 w-full items-center justify-center rounded-xl border bg-white/0 p-3 text-sm outline-none border-gray-200">
            <Icon
            :icon="passwordType1 === 'password' ? 'material-symbols:lock-outline' : 'material-symbols:lock-open-outline-rounded'"
            class="absolute flex flex-row right-3 bottom-3 text-2xl cursor-pointer"
            id="password-icon"
            v-on:click="(e) => togglePassword(e)"
            />
        </div>
        <div class="mb-4 relative">
            <label for="password-again" class="text-sm text-navy-700 ml-0.5">Password again</label>
            <input :type="passwordType2" id="password-again" class="mt-2 flex h-12 w-full items-center justify-center rounded-xl border bg-white/0 p-3 text-sm outline-none border-gray-200">
            <Icon
            :icon="passwordType2 === 'password' ? 'material-symbols:lock-outline' : 'material-symbols:lock-open-outline-rounded'"
            class="absolute flex flex-row right-3 bottom-3 text-2xl cursor-pointer"
            id="password-again-icon"
            v-on:click="(e) => togglePassword(e)"
            />
        </div>
        <div class="flex text-center items-center justify-center mb-4 relative" v-if="response.status !== null">
            <div class="text-xl text-green-500" v-if="response.status === true">
                User registered
            </div>
            <div class="text-xl text-red-500" v-else-if="response.status === 406">
                Passwords don't match
            </div>
            <div class="text-xl text-red-500" v-else-if="response.status === 400">
                User with this email already exists
            </div>
            <div class="text-xl text-red-500" v-else>
                Something went wrong
            </div>
        </div>
        <div class="flex items-center justify-center w-full min-h-[50px] mt-4 group">
          <button class="h-full w-2/3 border border-gray-300 text-black rounded-lg group-hover:bg-gray-300"
          v-if="response.loading === false"
          v-on:click="() => handleClick()">Register</button>
          <Icon 
          v-else
          icon="eos-icons:three-dots-loading"
          class="w-2/3 border border-gray-300 rounded-lg text-5xl"
          />
        </div>
      </div>
    </div>
  </template>
  
  <script>
  import { API_URL } from "../../config.js"
  import { Icon } from '@iconify/vue';
  import axios from 'axios'
  import { reactive } from "vue";
  
    export default {
      name: 'Register',
      components: {
          Icon
      },
      data() {
          return {
              passwordType1: "password",
              passwordType2: "password"
          }
      },
      methods: {
          togglePassword(e) {
              if (e.target.id === "password-icon") {
                  if (this.passwordType1 === "password") {
                      this.passwordType1 = "text";
                  } else {
                      this.passwordType1 = "password";
                  }
              } else if (e.target.id === "password-again-icon") {
                  if (this.passwordType2 === "password") {
                      this.passwordType2 = "text";
                  } else {
                      this.passwordType2 = "password";
                  }
              }
          }
      },
      setup() {
          const response = reactive({ 
              loading: false,
              status: null,
          });
  
          async function registerRequest(formData) {
              /* Register the user */
              response.loading = true;
  
              axios.post(`${API_URL}/api/user/register`, formData, {
                  headers: {
                      'Content-Type': 'application/json'
                  }
              })
                  .then(r => {
                      response.status = true;
                  })
                  .catch(error => {
                      if (error.response?.status === 400) {
                          response.status = 400;
                      } else {
                          response.status = false;
                      }
                  })
                  .finally(() => {
                      response.loading = false;
                  })
          }
  
          function handleClick() {
              /* Validate the data provided by the user */
              const email = document.getElementById("email");
              const firstName = document.getElementById("first-name");
              const lastName = document.getElementById("last-name");
              const password = document.getElementById("password");
              const password2 = document.getElementById("password-again");
              const fields = [email, firstName, lastName, password, password2];
              let errorFlag = false;
              const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  
              for (let field of fields) {
                  if (field === email && !emailRegex.test(field.value)) {
                      field.style.borderColor = "red";
                      errorFlag = true;
                  } else if (field.value === "") {
                      field.style.borderColor = "red";
                      errorFlag = true;
                  } else if (password.value !== password2.value && (field === password || field === password2)) {
                      password.style.borderColor = "red";
                      password2.style.borderColor = "red";
                      errorFlag = true;
                      response.status = 406;
                      console.log("here")
                  } else {
                      field.style.borderColor = "green";
                  }
                  // TODO check passwords match
              }
              
  
              if (!errorFlag) {
                  const formData = {};
                  formData.firstName = firstName.value;
                  formData.lastName = lastName.value;
                  formData.email = email.value;
                  formData.password = password.value;
                  registerRequest(formData);
              }
          }
  
          return {
              handleClick,
              response
          }
          }
    }
    </script>
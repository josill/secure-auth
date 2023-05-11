<template>
    <div class="flex flex-col flex-1 justify-center items-center bg-slate-100 font-montserrat">
      <div class="relative border-2 border-gray-300 shadow rounded-lg max-w-[300px] md:max-w-[400px] flex flex-col w-full !p-6 3xl:p-![18px] bg-white">   
        <div class="relative flex flex-row justify-between">
            <h4 class="text-xl font-bold text-navy-700 mb-6">
                Login
            </h4>
        </div>
        <div class="mb-4">
            <label for="email" class="text-sm text-navy-700 ml-0.5">Email</label>
            <input type="text" id="email" placeholder="bob@gmail.com" ref="email" class="mt-2 flex h-12 w-full items-center justify-center rounded-xl border bg-white/0 p-3 text-sm outline-none border-gray-200">
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
        <div class="flex text-center items-center justify-center mb-4 relative" v-if="response.status !== null">
            <div class="text-xl text-red-500" v-if="response.status === 400">
                Wrong password
            </div>
            <div class="text-xl text-red-500" v-else-if="response.status === 404">
                User does not exist
            </div>
            <div class="text-xl text-red-500" v-else-if="response.status === false">
                Something went wrong
            </div>
        </div>
        <div class="flex items-center justify-center w-full min-h-[50px] mt-4 group">
          <button class="h-full w-2/3 border border-gray-300 text-gray-400 rounded-lg group-hover:bg-gray-300 group-hover:text-gray-500"
          v-on:click="() => handleClick()">Login</button>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  import { API_URL } from "../../config.js"
  import { Icon } from '@iconify/vue';
  import axios from 'axios'
  import { reactive } from "vue";
  import { useRouter } from 'vue-router';
  
    export default {
      name: 'Login',
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
              }
          }
      },
      setup() {
          const response = reactive({ 
              loading: false,
              status: null,
          });
          const router = useRouter();
  
          async function loginRequest(formData) {
              /* Register the user */
              response.loading = true;
  
              axios.post(`${API_URL}/api/user/login`, formData, {
                  headers: {
                      'Content-Type': 'application/json'
                  }
              })
                  .then(r => {
                    if (r?.status === 200) {
                      router.push('/');
                    }
                  })
                  .catch(error => {
                      if (error.response?.status === 404) {
                          response.status = 404; // conflict
                      } else if (error.response?.status === 400 && error.response.data == "Wrong password.") {
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
            const email = document.getElementById("email"); 
            const password = document.getElementById("password");
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            let errorFlag = false;

            if (email.value === '' || !emailRegex.test(email.value)) {
              email.style.borderColor = "red";
              errorFlag = true;
            } else if (password.value === '') {
              password.style.borderColor = "red";
              errorFlag = true;
            }

            if (!errorFlag) {
              const formData = {};
              formData.email = email.value;
              formData.password = password.value;
              loginRequest(formData);
            }
          }
  
          return {
              handleClick,
              response
          }
          }
    }
    </script>
    
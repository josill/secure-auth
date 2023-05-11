<template>
    <div class="flex h-screen w-screen items-center justify-center font-Montserrat text-xl">
        <div class="flex flex-col items-center justify-center border-2 border-green-500 rounded-lg h-1/3 w-1/2 text-green-500"
        v-if="response.status === true">
            <div class="flex">
                <Icon icon="mdi:account-check-outline" class="mr-2 text-2xl" />
                <h1>Account confirmed</h1>
            </div>
            <router-link to="/login">
                <button class="flex items-center justify-center min-h-[50px] min-w-[200px] border border-green-300 rounded-lg hover:bg-green-200 mt-8"
                ><span class="flex">Login <Icon icon="material-symbols:arrow-right-alt" class="text-2xl mt-0.5" /></span></button>
            </router-link>
        </div>
        <div class="flex flex-col items-center justify-center border-2 border-red-500 rounded-lg h-1/3 w-1/2 text-red-500"
        v-else-if="response.status === 404">
            <div class="flex">
                <Icon icon="charm:circle-cross" class=" mr-2 mt-0.5 text-2xl" />
                <h1>Token invalid or not found</h1>
            </div>
            <button class="flex items-center justify-center min-h-[50px] min-w-[200px] border border-red-300 rounded-lg  hover:bg-red-200 mt-8"
            ><span class="flex">Get new token <Icon icon="material-symbols:arrow-right-alt" class="text-2xl mt-0.5" /></span></button>
        </div>
        <div class="flex flex-col items-center justify-center border-2 border-red-500 rounded-lg h-1/3 w-1/2 text-red-500 text-center"
        v-else-if="response.status === 409">
            <div class="flex">
                <Icon icon="charm:circle-cross" class=" mr-2 mt-0.5 text-2xl" />
                <h1>Email already confirmed</h1>
            </div>
        </div>
        <div class="flex flex-col items-center justify-center border-2 border-red-500 rounded-lg h-1/3 w-1/2 text-red-500 text-center"
        v-else-if="response.status === 202">
            <div class="flex w-11/12">
                <h1>Confirmation token expired. New token was sent to email</h1>
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
    name: 'ConfirmRegistration',
    props: ['token'],
    components: {
        Icon
    },
    data() {
    },
    methods: {
    },
    setup(props) {
        const response = reactive({ 
            loading: false,
            status: null,
        });

        function handleToken() {
            if (props.token && props.token !== null && props.token !== '') {
                confirmRequest();
            } else {
                response.status = false;
            }
        }

        async function confirmRequest() {
            /* Register the user */
            response.loading = true;

            axios.get(`${API_URL}/api/user/confirm?token=${props.token}`)
                .then(r => {
                    if (r.status === 202) {
                        response.status = 202;
                    } else {
                        response.status = true;
                    }
                })
                .catch(error => {
                    if (error.response?.status === 404) {
                        response.status = 404;
                    } else if (error.response?.status === 409) {
                        response.status = 409;
                    } else {
                        response.status = false;
                    }
                })
                .finally(() => {
                    response.loading = false;
                })
        }

        handleToken()
        return {
            response
        }
    },
    mounted() {
    }
  }
  </script>
  
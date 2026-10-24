import { App, Plugin } from 'vue';
import Button from './components/Button.vue';
import Input from './components/Input.vue';
import Modal from './components/Modal.vue';
import Card from './components/Card.vue';
import Table from './components/Table.vue';
import Form from './components/Form.vue';
import FormItem from './components/FormItem.vue';
import Avatar from './components/Avatar.vue';
import Badge from './components/Badge.vue';
import Tag from './components/Tag.vue';
import './styles/index.css';

const components = [
  Button,
  Input,
  Modal,
  Card,
  Table,
  Form,
  FormItem,
  Avatar,
  Badge,
  Tag
];

const install: Plugin = (app: App) => {
  components.forEach(component => {
    app.component(component.name || component.__name || '', component);
  });
};

export {
  Button,
  Input,
  Modal,
  Card,
  Table,
  Form,
  FormItem,
  Avatar,
  Badge,
  Tag,
  install
};

export default { install };
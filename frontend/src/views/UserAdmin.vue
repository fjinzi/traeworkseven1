<template>
  <div class="user-admin">
    <div class="header">
      <h1>用户管理</h1>
      <button class="btn-primary" @click="showCreateModal = true">
        <span class="icon">+</span> 添加用户
      </button>
    </div>

    <div class="search-bar">
      <input
        v-model="searchForm.username"
        type="text"
        placeholder="用户名"
        @keyup.enter="handleSearch"
      />
      <input
        v-model="searchForm.nickname"
        type="text"
        placeholder="昵称"
        @keyup.enter="handleSearch"
      />
      <select v-model="searchForm.roleType">
        <option value="">全部角色</option>
        <option value="0">普通用户</option>
        <option value="1">管理员</option>
      </select>
      <button class="btn-secondary" @click="handleSearch">搜索</button>
      <button class="btn-text" @click="resetSearch">重置</button>
    </div>

    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>邮箱</th>
            <th>手机号</th>
            <th>角色</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in userList" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.nickname || '-' }}</td>
            <td>{{ user.email || '-' }}</td>
            <td>{{ user.phone || '-' }}</td>
            <td>
              <span :class="['role-tag', user.roleType === 1 ? 'admin' : 'user']">
                {{ user.roleType === 1 ? '管理员' : '普通用户' }}
              </span>
            </td>
            <td>{{ formatDate(user.createTime) }}</td>
            <td>
              <button class="btn-text" @click="handleEdit(user)">编辑</button>
              <button
                v-if="user.username !== 'admin'"
                class="btn-text danger"
                @click="handleDelete(user)"
              >
                删除
              </button>
            </td>
          </tr>
          <tr v-if="userList.length === 0">
            <td colspan="8" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pagination" v-if="total > 0">
      <button
        :disabled="currentPage === 1"
        @click="handlePageChange(currentPage - 1)"
      >
        上一页
      </button>
      <span>第 {{ currentPage }} 页 / 共 {{ totalPages }} 页</span>
      <button
        :disabled="currentPage === totalPages"
        @click="handlePageChange(currentPage + 1)"
      >
        下一页
      </button>
    </div>

    <!-- 创建用户弹窗 -->
    <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="modal">
        <h2>添加用户</h2>
        <form @submit.prevent="handleCreateSubmit">
          <div class="form-group">
            <label>用户名 <span class="required">*</span></label>
            <input
              v-model="createForm.username"
              type="text"
              required
              minlength="3"
              maxlength="20"
              placeholder="3-20个字符，支持字母、数字、下划线"
            />
          </div>
          <div class="form-group">
            <label>密码 <span class="required">*</span></label>
            <input
              v-model="createForm.password"
              type="password"
              required
              minlength="6"
              maxlength="20"
              placeholder="6-20个字符"
            />
          </div>
          <div class="form-group">
            <label>昵称</label>
            <input
              v-model="createForm.nickname"
              type="text"
              maxlength="50"
              placeholder="选填"
            />
          </div>
          <div class="form-group">
            <label>邮箱</label>
            <input
              v-model="createForm.email"
              type="email"
              placeholder="选填"
            />
          </div>
          <div class="form-group">
            <label>手机号</label>
            <input
              v-model="createForm.phone"
              type="tel"
              placeholder="选填"
            />
          </div>
          <div class="form-group">
            <label>角色</label>
            <select v-model="createForm.roleType">
              <option :value="0">普通用户</option>
              <option :value="1">管理员</option>
            </select>
          </div>
          <div class="form-actions">
            <button type="button" class="btn-text" @click="showCreateModal = false">取消</button>
            <button type="submit" class="btn-primary" :disabled="creating">确定</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 编辑用户弹窗 -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="modal">
        <h2>编辑用户</h2>
        <form @submit.prevent="handleEditSubmit">
          <div class="form-group">
            <label>用户名</label>
            <input :value="editForm.username" type="text" disabled />
          </div>
          <div class="form-group">
            <label>昵称</label>
            <input
              v-model="editForm.nickname"
              type="text"
              maxlength="50"
              placeholder="选填"
            />
          </div>
          <div class="form-group">
            <label>邮箱</label>
            <input
              v-model="editForm.email"
              type="email"
              placeholder="选填"
            />
          </div>
          <div class="form-group">
            <label>手机号</label>
            <input
              v-model="editForm.phone"
              type="tel"
              placeholder="选填"
            />
          </div>
          <div class="form-group">
            <label>角色</label>
            <select v-model="editForm.roleType">
              <option :value="0">普通用户</option>
              <option :value="1">管理员</option>
            </select>
          </div>
          <div class="form-actions">
            <button type="button" class="btn-text" @click="showEditModal = false">取消</button>
            <button type="submit" class="btn-primary" :disabled="updating">确定</button>
          </div>
        </form>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="showDeleteModal = false">
      <div class="modal confirm-modal">
        <h2>确认删除</h2>
        <p>确定要删除用户 "{{ deleteTarget?.username }}" 吗？</p>
        <p class="warning">此操作将逻辑删除该用户，数据仍可恢复。</p>
        <div class="form-actions">
          <button class="btn-text" @click="showDeleteModal = false">取消</button>
          <button class="btn-danger" :disabled="deleting" @click="confirmDelete">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { userAdminApi, type UserInfo, type UserCreateRequest, type UserUpdateRequest } from '@/api/userAdmin'

const userList = ref<UserInfo[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(0)

const searchForm = reactive({
  username: '',
  nickname: '',
  roleType: ''
})

const showCreateModal = ref(false)
const showEditModal = ref(false)
const showDeleteModal = ref(false)
const creating = ref(false)
const updating = ref(false)
const deleting = ref(false)

const createForm = reactive<UserCreateRequest>({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  roleType: 0
})

const editForm = reactive<UserUpdateRequest & { id?: number; username?: string }>({
  id: undefined,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  roleType: 0
})

const deleteTarget = ref<UserInfo | null>(null)

const fetchUsers = async () => {
  try {
    const params = {
      username: searchForm.username || undefined,
      nickname: searchForm.nickname || undefined,
      roleType: searchForm.roleType !== '' ? parseInt(searchForm.roleType) : undefined,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    }

    const result = await userAdminApi.getUsers(params)
    if (result.success && result.data) {
      userList.value = result.data.list
      total.value = result.data.total
      totalPages.value = result.data.totalPages
    } else {
      alert(result.message || '获取用户列表失败')
    }
  } catch (error: any) {
    alert(error.message || '获取用户列表失败')
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchUsers()
}

const resetSearch = () => {
  searchForm.username = ''
  searchForm.nickname = ''
  searchForm.roleType = ''
  currentPage.value = 1
  fetchUsers()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchUsers()
}

const handleCreateSubmit = async () => {
  if (!createForm.username || !createForm.password) {
    alert('请填写必填项')
    return
  }

  creating.value = true
  try {
    const result = await userAdminApi.createUser(createForm)
    if (result.success) {
      alert('创建用户成功')
      showCreateModal.value = false
      resetCreateForm()
      fetchUsers()
    } else {
      alert(result.message || '创建用户失败')
    }
  } catch (error: any) {
    alert(error.message || '创建用户失败')
  } finally {
    creating.value = false
  }
}

const handleEdit = (user: UserInfo) => {
  editForm.id = user.id
  editForm.username = user.username
  editForm.nickname = user.nickname || ''
  editForm.email = user.email || ''
  editForm.phone = user.phone || ''
  editForm.roleType = user.roleType
  showEditModal.value = true
}

const handleEditSubmit = async () => {
  if (!editForm.id) return

  updating.value = true
  try {
    const data: UserUpdateRequest = {
      nickname: editForm.nickname,
      email: editForm.email,
      phone: editForm.phone,
      roleType: editForm.roleType
    }
    const result = await userAdminApi.updateUser(editForm.id, data)
    if (result.success) {
      alert('更新用户成功')
      showEditModal.value = false
      fetchUsers()
    } else {
      alert(result.message || '更新用户失败')
    }
  } catch (error: any) {
    alert(error.message || '更新用户失败')
  } finally {
    updating.value = false
  }
}

const handleDelete = (user: UserInfo) => {
  deleteTarget.value = user
  showDeleteModal.value = true
}

const confirmDelete = async () => {
  if (!deleteTarget.value) return

  deleting.value = true
  try {
    const result = await userAdminApi.deleteUser(deleteTarget.value.id)
    if (result.success) {
      alert('删除用户成功')
      showDeleteModal.value = false
      fetchUsers()
    } else {
      alert(result.message || '删除用户失败')
    }
  } catch (error: any) {
    alert(error.message || '删除用户失败')
  } finally {
    deleting.value = false
  }
}

const resetCreateForm = () => {
  createForm.username = ''
  createForm.password = ''
  createForm.nickname = ''
  createForm.email = ''
  createForm.phone = ''
  createForm.roleType = 0
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-admin {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h1 {
  margin: 0;
  font-size: 24px;
  color: #333;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.search-bar input,
.search-bar select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.search-bar input {
  width: 150px;
}

.table-container {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

.data-table th {
  background: #f5f5f5;
  font-weight: 600;
  color: #666;
}

.data-table tr:hover {
  background: #f9f9f9;
}

.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}

.role-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.role-tag.user {
  background: #e3f2fd;
  color: #1976d2;
}

.role-tag.admin {
  background: #fff3e0;
  color: #f57c00;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
  margin-top: 20px;
}

.pagination button {
  padding: 6px 12px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 4px;
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  padding: 8px 16px;
  background: #4caf50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-primary:hover {
  background: #45a049;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 8px 16px;
  background: #2196f3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-secondary:hover {
  background: #1976d2;
}

.btn-danger {
  padding: 8px 16px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.btn-danger:hover {
  background: #d32f2f;
}

.btn-danger:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-text {
  padding: 6px 12px;
  background: transparent;
  color: #2196f3;
  border: none;
  cursor: pointer;
  font-size: 14px;
}

.btn-text:hover {
  color: #1976d2;
}

.btn-text.danger {
  color: #f44336;
}

.btn-text.danger:hover {
  color: #d32f2f;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  border-radius: 8px;
  padding: 24px;
  width: 100%;
  max-width: 450px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal h2 {
  margin: 0 0 20px 0;
  font-size: 18px;
  color: #333;
}

.confirm-modal {
  text-align: center;
}

.confirm-modal p {
  margin: 10px 0;
  color: #666;
}

.confirm-modal .warning {
  color: #f57c00;
  font-size: 14px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #555;
}

.form-group label .required {
  color: #f44336;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group input:disabled {
  background: #f5f5f5;
  color: #999;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.icon {
  font-size: 18px;
}
</style>

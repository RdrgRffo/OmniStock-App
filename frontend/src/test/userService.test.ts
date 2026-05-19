import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getUsers, getUserDetails, createUser, updateUser, deleteUser } from 'features/users/services/userService';

const { mockGet, mockPost, mockPut, mockDelete } = vi.hoisted(() => ({
  mockGet: vi.fn(),
  mockPost: vi.fn(),
  mockPut: vi.fn(),
  mockDelete: vi.fn(),
}));

vi.mock('@/services/api', () => ({
  default: {
    get: mockGet,
    post: mockPost,
    put: mockPut,
    delete: mockDelete,
  },
}));

describe('userService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getUsers', () => {
    it('debe hacer GET a /admin/usuarios', async () => {
      mockGet.mockResolvedValue({ data: { data: [{ id: 1, username: 'admin', email: 'admin@test.com', roles: ['ROLE_ADMIN'] }] } });
      const result = await getUsers();
      expect(mockGet).toHaveBeenCalledWith('/admin/usuarios');
      expect(result).toHaveLength(1);
      expect(result[0].username).toBe('admin');
    });
  });

  describe('getUserDetails', () => {
    it('debe hacer GET a /admin/usuarios/:id', async () => {
      mockGet.mockResolvedValue({ data: { data: { id: 1, username: 'admin', email: 'admin@test.com', roles: ['ROLE_ADMIN'] } } });
      const result = await getUserDetails(1);
      expect(mockGet).toHaveBeenCalledWith('/admin/usuarios/1');
      expect(result.id).toBe(1);
    });
  });

  describe('createUser', () => {
    it('debe hacer POST a /admin/usuarios', async () => {
      const user = { username: 'newuser', email: 'new@test.com', password: 'pass123', roles: ['ROLE_CLIENTE'] };
      mockPost.mockResolvedValue({ data: { data: { id: 1, ...user } } });
      const result = await createUser(user);
      expect(mockPost).toHaveBeenCalledWith('/admin/usuarios', user);
      expect(result.id).toBe(1);
    });
  });

  describe('updateUser', () => {
    it('debe hacer PUT a /admin/usuarios/:id', async () => {
      const user = { username: 'updated', email: 'updated@test.com', roles: ['ROLE_CLIENTE'] };
      mockPut.mockResolvedValue({ data: { data: { id: 1, ...user } } });
      const result = await updateUser(1, user);
      expect(mockPut).toHaveBeenCalledWith('/admin/usuarios/1', user);
      expect(result.username).toBe('updated');
    });
  });

  describe('deleteUser', () => {
    it('debe hacer DELETE a /admin/usuarios/:id', async () => {
      mockDelete.mockResolvedValue({});
      await deleteUser(1);
      expect(mockDelete).toHaveBeenCalledWith('/admin/usuarios/1');
    });
  });
});
